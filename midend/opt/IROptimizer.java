package midend.opt;

import midend.IRModule;
import midend.Value.*;
import midend.Value.Instructions.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import static midend.opt.DagNode.DAGType.*;

public class IROptimizer {
    private final IRModule module;
    private final BufferedWriter op_outputWriter;
    private final Map<Function, DefUseNet> defUseNetMap = new HashMap<>();
    private final Map<Value, Value> realRef = new HashMap<>();

    public IROptimizer(IRModule module, BufferedWriter op_outputWriter) {
        this.module = module;
        this.op_outputWriter = op_outputWriter;
    }

    public IRModule optimize() throws IOException {
        //死代码删除-1
        List<Function> livingFunctions = killUnreachableCodes(module.getMainFunc());
        IRModule opt_module = new IRModule(module.getGlobalVarList(), livingFunctions);
        //消除公共表达式
        combineCommonExpressionInBlock(opt_module);
        //死代码删除-2
        buildUseDefInAllBlock(opt_module.getFunctionList());
        getActiveFlowInAllBlock(opt_module.getFunctionList());
        //死代码删除-3
        killDeadCode(opt_module.getFunctionList());
        //重新做一遍活跃变量分析
        buildUseDefInAllBlock(opt_module.getFunctionList());
        getActiveFlowInAllBlock(opt_module.getFunctionList());
        //标记叶子函数
        setLeafFunctions(opt_module);
        //计数引用
        calRefCount(opt_module);
        //建立冲突图
        getConflictGraph(opt_module);
        opt_module.printModule(op_outputWriter);
        return opt_module;
    }

    private void getConflictGraph(IRModule optModule) {
        for(Function function : optModule.getFunctionList()){
            Map<Value, Set<Value>> conflictGraph = new HashMap<>();
            Set<Value> globals = new HashSet<>(function.getRefCounter());
            for(BasicBlock block : function.getBlocks()){
                Set<Value> out = block.getOut();
                Set<Value> in = block.getIn();
                //System.out.println(block.getMIPSLabel()+" in "+in);
                //System.out.println(block.getMIPSLabel()+" out "+out);
                Set<Value> valid = new HashSet<>();
                valid.addAll(out);
                valid.addAll(in);
//                Set<Value> newValid = new HashSet<>();
//                for(Value value: valid){
//                    newValid.add(value);
//                    if(realRef.containsKey(value))
//                        newValid.add(realRef.get(value));
//                }
//                valid = newValid;
                //System.out.println(block.getMIPSLabel()+" in+out:"+valid);
//                System.out.println("globals:"+globals);
                valid.retainAll(globals);
                for(Value v1:valid){
                    for(Value v2:valid){
                        if(v1!=v2){
                            if(!conflictGraph.containsKey(v1))
                                conflictGraph.put(v1,new HashSet<>());
                            conflictGraph.get(v1).add(v2);
                        }else {
                            if(!conflictGraph.containsKey(v1))
                                conflictGraph.put(v1,new HashSet<>());
                        }
                    }
                }
            }
            function.setConflictGraph(conflictGraph);
            System.out.println("conflict graph for "+function.getName());
            for(Map.Entry<Value, Set<Value>> entry :conflictGraph.entrySet()){
                System.out.println(entry);
            }
        }
    }

    private void calRefCount(IRModule optModule){
        for(Function function : optModule.getFunctionList()){
            calRefCountInFunction(function);
        }
    }

    private void calRefCountInFunction(Function function){
        Map<Value, Integer> counter = new HashMap<>();
        for(BasicBlock block : function.getBlocks()){
            for(Instruction inst : block.getInstList()){
                if(inst instanceof AllocaInst){
                    Value global = inst.getResult();
                    if(!global.getType().isArray() || (global.getType().isArray() && global.getType().dePointer().isPointer()))
                        counter.put(inst.getResult(), 0);
                }else {
                    for(IRValue value : inst.getValueList()){
                        if(value instanceof Value v && !(value instanceof GlobalVar) && !(value instanceof ConstVal)){
                            if(counter.containsKey(v)){
                                int cur = counter.get(v);
                                cur+= block.isInLoop()?20:1;
                                counter.put(v, cur);
                            }
                        }
                    }
                }
            }
        }
        System.out.println(function.getName()+ " global counter "+counter);
        function.setRefCounter(counter);
//        System.out.println("Ref count in func: "+function.getName());
//        System.out.println(counter);
    }

    private void combineCommonExpressionInBlock(IRModule optModule) {
        for (Function function : optModule.getFunctionList()) {
            //System.out.println("==================="+function.getName()+"===================");
            BasicBlock inBlock = null;
            if (!function.getName().equals("@main")) {
                inBlock = function.getBlocks().get(0);
            }
            for (BasicBlock block : function.getBlocks()) {
                if (block != inBlock)
                    getDAG2(block);
            }
        }
    }

    public void getDAG2(BasicBlock block) {
        DAG dag = new DAG();
        Map<Value, Value> valueMap = new HashMap<>();
        Set<Instruction> instSet = new HashSet<>();
        for (Instruction inst : block.getInstList()) {
            if (inst instanceof IcmpInst) {
                Value res = inst.getResult();
                Value lVar = ((BinaryInst) inst).lVar;
                Value rVar = ((BinaryInst) inst).rVar;
                lVar = getRealValue(valueMap, lVar);
                rVar = getRealValue(valueMap, rVar);
                if (lVar == null || rVar == null) {
                    instSet.add(inst);
                    continue;
                }
                DagNode.DAGType type = null;
                switch (((IcmpInst) inst).operation) {
                    case GRE -> type = GRE;
                    case LSS -> {
                        type = GRE;
                        Value t = lVar;
                        lVar = rVar;
                        rVar = t;
                    }
                    case GEQ -> type = GEQ;
                    case LEQ -> {
                        type = GEQ;
                        Value t = lVar;
                        lVar = rVar;
                        rVar = t;
                    }
                    case EQL -> type = EQL;
                    case NEQ -> type = NEQ;
                }
                int i = dag.getValueIndex(lVar);
                int j = dag.getValueIndex(rVar);
                int k;
                DagNode mid = dag.findMidNode(type, inst, List.of(i, j));
                if (mid == null) {
                    mid = dag.createMidNode(type, inst, List.of(i, j));
                }
                k = mid.getIndex();
                dag.setValueIndex(res, k);
                valueMap.put(res, res);
            } else if (inst instanceof BinaryInst) {
                Value res = inst.getResult();
                Value lVar = ((BinaryInst) inst).lVar;
                Value rVar = ((BinaryInst) inst).rVar;
                lVar = getRealValue(valueMap, lVar);
                rVar = getRealValue(valueMap, rVar);
                if (lVar == null || rVar == null) {
                    instSet.add(inst);
                    continue;
                }
                int i = dag.getValueIndex(lVar);
                int j = dag.getValueIndex(rVar);
                int k;
                DagNode mid = dag.findMidNode(translate(((BinaryInst) inst).type), inst, List.of(i, j));
                if (mid == null) {
                    mid = dag.createMidNode(translate(((BinaryInst) inst).type), inst, List.of(i, j));
                }
                k = mid.getIndex();
                dag.setValueIndex(res, k);
                valueMap.put(res, res);
            } else if (inst instanceof ZextInst) {
                Value res = inst.getResult();
                Value lVar = ((ZextInst) inst).source;
                lVar = getRealValue(valueMap, lVar);
                if (lVar == null) {
                    instSet.add(inst);
                    continue;
                }
                int i = dag.getValueIndex(lVar);
                int k;
                DagNode mid = dag.findMidNode(ZEXT, inst, List.of(i));
                if (mid == null) {
                    mid = dag.createMidNode(ZEXT, inst, List.of(i));
                }
                k = mid.getIndex();
                dag.setValueIndex(res, k);
                valueMap.put(res, res);
            } else if (inst instanceof LoadInst) {
                Value res = inst.getResult();
                Value lVar = ((LoadInst) inst).source;
                lVar = getRealValue(valueMap, lVar);
                if (lVar != null) {
                    valueMap.put(res, lVar);
                }
                instSet.add(inst);
            } else if (inst instanceof AllocaInst) {
                Value res = inst.getResult();
                if (!res.getType().dePointer().isPointer())
                    valueMap.put(res, res);
                instSet.add(inst);
            } else if (inst instanceof StoreInst) {
                Value lVar = ((StoreInst) inst).lVar;
                Value rVar = ((StoreInst) inst).rVar;
                lVar = getRealValue(valueMap, lVar);
                if (lVar != null) {
                    valueMap.put(rVar, lVar);
                }
                instSet.add(inst);
            } else {
                instSet.add(inst);
            }
        }
        block.setInstList(getDagInst2(dag, instSet, block.getInstList()));
        //dag.printTable();
        //dag.printGraph();
    }

    private List<Instruction> getDagInst2(DAG dag, Set<Instruction> instSet, List<Instruction> instList) {
        List<Instruction> newList = new ArrayList<>();
        Map<Integer, Value> valueRef = new HashMap<>();
        Map<Value, Instruction> insRef = new HashMap<>();
        for (Instruction inst : instList) {
            if (instSet.contains(inst)) {
                newList.add(inst);
            } else {
                Value res = inst.getResult();
                int index = dag.findValueIndex(res);
                if (valueRef.containsKey(index)) {
                    Instruction move = new MoveInst(valueRef.get(index), res);
                    User.DeleteUser(inst);
                    int i = newList.indexOf(insRef.get(valueRef.get(index)));
                    newList.add(i + 1, move);
                } else {
                    newList.add(inst);
                    valueRef.put(index, res);
                    insRef.put(res, inst);
                }
            }
        }
        return newList;
    }

    public Value getRealValue(Map<Value, Value> valueMap, Value value) {
        Value res = value;
        while (valueMap.containsKey(res)) {
            if (res == valueMap.get(res))
                break;
            res = valueMap.get(res);
        }
        return res;
    }

    private DagNode.DAGType translate(BinaryInst.BinaryInstType type) {
        DagNode.DAGType res = null;
        if (type != null)
            switch (type) {
                case ADD -> res = DagNode.DAGType.ADD;
                case SDIV -> res = DagNode.DAGType.SDIV;
                case MUL -> res = DagNode.DAGType.MUL;
                case SUB -> res = DagNode.DAGType.SUB;
                case SREM -> res = DagNode.DAGType.SREM;
            }
        return res;
    }

    public List<Function> killUnreachableCodes(Function main) {
        Set<Function> visitedFunctions = new HashSet<>();
        killUnreachableCodesInFunction(main, visitedFunctions);
        List<Function> functions = module.getFunctionList();
        List<Function> newFunctions = new ArrayList<>();
        for (Function function : functions) {
            if (visitedFunctions.contains(function)) {
                newFunctions.add(function);
            } else {
                System.out.println("kill Function " + function.getName());
            }
        }
        return newFunctions;
    }

    public void killUnreachableCodesInFunction(Function function, Set<Function> visitedFunctions) {
        if (visitedFunctions.contains(function))
            return;
        visitedFunctions.add(function);
        List<BasicBlock> newBlocks = new ArrayList<>();
        Set<BasicBlock> visitedBlocks = new HashSet<>();
        if (function.getBlocks().isEmpty())
            return;
        BasicBlock inBlock = function.getBlocks().get(0);
        //得到所有活着的Blocks
        visitAllBlocks(inBlock, visitedBlocks);
        if (function.getName().equals("@main"))
            inBlock = null;
        //删除本函数的死代码
        //step1:删除全部的死block
        for (BasicBlock block : function.getBlocks()) {
            if (visitedBlocks.contains(block)) {
                newBlocks.add(block);
            } else {
                System.out.println("kill block " + block.getLabel());
                block.selfDelete();
            }
        }
        function.setBlocks(newBlocks);
        //step2:删除死代码，顺便得到本函数调用的函数
        List<Function> calls = new ArrayList<>();
        visitedBlocks.clear();
//        for (BasicBlock block : newBlocks) {
//            if (block.getSuccessors().isEmpty()) {
//                killDeadCodesInBlock(block, visitedBlocks, calls, inBlock);
//            }
//        }
        //step2:得到本函数调用的函数
        for (BasicBlock block : newBlocks) {
            if (block.getSuccessors().isEmpty()) {
                getAllCalls(block, visitedBlocks, calls);
            }
        }
        //递归地访问删除所有有效函数的不可达代码
        for (Function f : calls) {
            killUnreachableCodesInFunction(f, visitedFunctions);
        }
    }

    private void getAllCalls(BasicBlock block, Set<BasicBlock> visitedBlocks, List<Function> calls) {
        if (visitedBlocks.contains(block))
            return;
        visitedBlocks.add(block);
        for (Instruction inst : block.getInstList()) {
            if (inst instanceof CallInst call) {
                calls.add(call.function);
            }
        }
        for (BasicBlock pre : block.getPredecessors()) {
            getAllCalls(pre, visitedBlocks, calls);
        }
    }

    private void visitAllBlocks(BasicBlock curBlock, Set<BasicBlock> visitedBlocks) {
        if (visitedBlocks.contains(curBlock))
            return;
        visitedBlocks.add(curBlock);
        for (BasicBlock block : curBlock.getSuccessors()) {
            visitAllBlocks(block, visitedBlocks);
        }
    }

    private void buildUseDefInAllBlock(List<Function> functions) {
        //每次创建都会建新的网
        defUseNetMap.clear();
        for (Function function : functions) {
            //System.out.println("def-use-func: "+function.getName()+"============");
            DefUseNet net = new DefUseNet();
            defUseNetMap.put(function, net);
            for (BasicBlock block : function.getBlocks()) {
                DefUseNode node = net.getNode(block);
                //System.out.println("def-use-block: "+block.getLabel());
                for (Instruction inst : block.getInstList()) {
                    for (IRValue value : inst.getValueList()) {
                        if (value instanceof Value v && !(value instanceof ConstVal) && !(value instanceof GlobalVar)) {
                            node.insertUse((Value) value, inst);
                            if (!node.getDef().contains(v))
                                node.getUse().add(v);
                        }
                    }
                    Value res = inst.getResult();
                    if (res != null) node.insertDef(res, inst);
                    if (res != null && !node.getUse().contains(res)) {
                        node.getDef().add(res);
                    }
                }

                //System.out.println("block: "+function.getName()+block);
                //System.out.println("use "+node.getUse());
                //System.out.println("def "+node.getDef());

            }
        }
    }

    private void getActiveFlowInAllBlock(List<Function> functions) {
        for (Function function : functions) {
            DefUseNet net = defUseNetMap.get(function);
            boolean notSame;
            do {
                notSame = false;
                Set<BasicBlock> visited = new HashSet<>();
                for (BasicBlock out : function.getBlocks()) {
                    if (out.getSuccessors().isEmpty()) {
                        notSame = getActiveFlow(visited, out, net) || notSame;
                    }
                }
                visited.clear();
            } while (notSame);
            for (BasicBlock out : function.getBlocks()) {
                DefUseNode node = net.getNode(out);
                out.setIn(node.getIn());
                out.setOut(node.getOut());
                System.out.println("block: "+function.getName()+out);
                System.out.println("in "+node.getIn());
                System.out.println("out "+node.getOut());
            }
        }
    }

    private boolean getActiveFlow(Set<BasicBlock> visited, BasicBlock block, DefUseNet net) {
        boolean modified = false;
        Set<Value> out = new HashSet<>();
        DefUseNode node = net.getNode(block);
        for (BasicBlock next : block.getSuccessors()) {
            out.addAll(net.getNode(next).getIn());
        }
        node.getOut().addAll(out);
        Set<Value> tmp = new HashSet<>(out);
        tmp.removeAll(node.getDef());
        tmp.addAll(node.getUse());
        if (!node.getIn().equals(tmp)) {
            modified = true;
            node.getIn().addAll(tmp);
        }
        if (!visited.contains(block)) {
            visited.add(block);
            for (BasicBlock pre : block.getPredecessors()) {
                modified = getActiveFlow(visited, pre, net) || modified;
            }
        }
        return modified;
    }

    private void killDeadCode(List<Function> functions) {
        for (Function function : functions) {
            System.out.println("=======Opt " + function.getMIPSName() + "=======");
            DefUseNet net = defUseNetMap.get(function);
            Set<BasicBlock> visitedBlocks = new HashSet<>();
            Map<Instruction, BasicBlock> initSet = new HashMap<>();
            int i = 1;
            if (function.getName().equals("@main"))
                i = 0;
            for (; i < function.getBlocks().size(); i++) {
                BasicBlock block = function.getBlocks().get(i);
                if (block.getSuccessors().isEmpty()) {
                    getAllII(block, visitedBlocks, initSet);
                }
            }
            Set<Instruction> validInst = new HashSet<>();
            for (Map.Entry<Instruction, BasicBlock> entry : initSet.entrySet()) {
                //System.out.println("============debug: "+entry.getKey()+" ============");
                visitAllInst(entry.getKey(), entry.getValue(), validInst, net);
            }
            i = 1;
            if (function.getName().equals("@main"))
                i = 0;
            for (; i < function.getBlocks().size(); i++) {
                BasicBlock block = function.getBlocks().get(i);
                List<Instruction> newList = new ArrayList<>();
                for (Instruction inst : block.getInstList()) {
                    if (validInst.contains(inst)) {
                        newList.add(inst);
                    } else {
                        System.out.println("死代码删除 " + inst);
                        User.DeleteUser(inst);
                    }
                }
                block.setInstList(newList);
            }
        }
    }

    private void getAllII(BasicBlock block, Set<BasicBlock> visitedBlocks, Map<Instruction, BasicBlock> initSet) {
        if (visitedBlocks.contains(block))
            return;
        visitedBlocks.add(block);
        for (Instruction inst : block.getInstList()) {
            if (inst instanceof CallInst
//                    || inst instanceof AllocaInst
                    || inst instanceof BrInst
                    || inst instanceof RetInst
//                    || inst instanceof GetElementPtrInst
                    || isModifyGlobal(inst))
                initSet.put(inst, block);
        }
        if (block.getEndInst() != null)
            initSet.put(block.getEndInst(), block);
        for (BasicBlock pre : block.getPredecessors()) {
            getAllII(pre, visitedBlocks, initSet);
        }
    }

    private void visitAllInst(Instruction inst, BasicBlock block, Set<Instruction> validInst, DefUseNet net) {
        if (validInst.contains(inst)) return;
        validInst.add(inst);
        //System.out.println(inst.getValueList());

        for (IRValue value : inst.getValueList()) {
            if (value instanceof Value && !(value instanceof ConstVal) && !(value instanceof GlobalVar)) {
                Map<Instruction, BasicBlock> definers = net.getDefiner((Value) value, inst, block);
                for (Map.Entry<Instruction, BasicBlock> entry : definers.entrySet()) {
                    //System.out.println("visit "+entry.getKey());
                    visitAllInst(entry.getKey(), entry.getValue(), validInst, net);

                }
            }
        }
    }

    private boolean isModifyGlobal(Instruction inst) {
        if (inst instanceof StoreInst storeInst) {
            return storeInst.getResult() instanceof GlobalVar || storeInst.getResult().getType().dePointer().isPointer();
        }
        return false;
    }

    private void setLeafFunctions(IRModule module){
        Set<Function> inlineFuncs = new HashSet<>();
        boolean isLeaf = true;
        for(Function function : module.getFunctionList()){
            for(BasicBlock block : function.getBlocks()){
                for(Instruction inst : block.getInstList()){
                    if(inst instanceof CallInst && !((CallInst) inst).function.isLibFunc()){
                        isLeaf = false;
                        break;
                    }
                }
                if(!isLeaf)
                    break;
            }
            if(isLeaf)
                inlineFuncs.add(function);
        }
        //inline那些可以inline的函数
        for(Function function : inlineFuncs){
            function.setLeaf();
            System.out.println("leaf func: "+function.getName());
        }
    }

//
//    private void inlineFunc(Function function, Set<Function> inlineFuncs){
//        List<BasicBlock> newblocks = new ArrayList<>();
//        for(BasicBlock block : function.getBlocks()){
//            int i = 0;
//            for(; i<block.getInstList().size();i++){
//                Instruction inst = block.getInstList().get(i);
//                if(inst instanceof CallInst && inlineFuncs.contains(((CallInst) inst).function)){
//                    //处理函数
//                    Function inlineFunc = ((CallInst) inst).function;
//                    //这里注意，需要对blocks和instructions进行深拷贝
//                    List<BasicBlock> blocks = inlineFunc.getBlocks();
//                    BasicBlock inblock = blocks.get(0);
//                    List<BasicBlock> outblock = new ArrayList<>();
//                    for(BasicBlock b : blocks){
//                        //顺便修改全部的block的名字
//                        b.setLabel(block.getLabel()+"_inline_"+inlineFunc.getName()+"_"+b.getLabel());
//                        if(b.getSuccessors().isEmpty())
//                            outblock.add(b);
//                    }
//                    BasicBlock nextBlock = new BasicBlock(block.getLabel()+"_inline_"+inlineFunc.getName()+"_exit");
//                    nextBlock.setParent(function);
//                    Instruction end = block.getRealEndInst();
//                    //call之前的不变
//                    block.setInstList(block.getInstList().subList(0,i));
//                    block.setEndInst(new BrInst(inblock));
//                    //维护跳转关系
//                    block.getSuccessors().clear();
//                    block.getSuccessors().add(inblock);
//                    newblocks.add(block);
//                    //TODO：维护跳转关系/修改传参/修改ret
//                    newblocks.addAll(blocks);
//                    for(BasicBlock out : outblock){
//                        out.getSuccessors().add(nextBlock);
//                        out.getInstList().remove(out.getInstList().size()-1);
//                        out.getInstList().add(new BrInst(nextBlock));
//                        //TODO:维护跳转关系
//                    }
//                    //call之后的放进新块
//                    if(i+1<block.getInstList().size())
//                        nextBlock.setInstList(block.getInstList().subList(i+1, block.getInstList().size()));
//                    else
//                        nextBlock.setInstList(new ArrayList<>());
//                    nextBlock.getInstList().add(end);
//                    nextBlock.setBlockEnd();
//                    newblocks.add(nextBlock);
//                    break;
//                }
//            }
//            if(i == block.getInstList().size())
//                newblocks.add(block);
//        }
//        function.setBlocks(newblocks);
//    }
//
//    private String getUniqueInlineName(String block, String symbol){
//        return block + "_inline_" + symbol + "_" + cnt;
//    }

}