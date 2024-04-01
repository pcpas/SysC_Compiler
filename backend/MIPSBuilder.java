package backend;

import midend.IRModule;
import midend.Value.*;
import midend.Value.Instructions.*;
import backend.MIPSInst.*;
import backend.MIPSTable.MFunctionSymbol;
import backend.MIPSTable.MTable;
import backend.MIPSTable.MVarSymbol;
import backend.MIPSValues.*;
import frontend.Tokens;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class MIPSBuilder {
    private final BufferedWriter writer;
    private final IRModule irModule;
    private final MModule module;
    private final RegisterManager registerManager;
    private final HashMap<String, MBasicBlock> blockMap = new HashMap<>();

    private final Map<MBasicBlock, TreeSet<Register>> blockLiveGlobalRegs = new HashMap<>();

    private final Map<Function, MFunction> functionMap = new HashMap<>();

    private MFunction curFunction = null;
    private MTable curTable = new MTable(null);

    public MIPSBuilder(BufferedWriter writer, IRModule module) {
        this.writer = writer;
        this.irModule = module;
        this.module = new MModule(curTable);
        this.registerManager = new RegisterManager();
    }

    public MModule generateMIPS() throws IOException {

        translateIR();
        //debug
        //curTable.debug(0);
        System.out.println("=====check free reg=====");
        printAllSavedReg();
        module.printMIPSModule(writer);
        return module;
    }

    public void translateIR() {
        buildGlobalVar(irModule.getGlobalVarList());
        for (Function function : irModule.getFunctionList()) {
            translateFunction(function);
        }
//        for (MFunction function : module.getFunctionList()) {
//            manageRegsInFunction(function);
//        }
    }

    public void translateFunction(Function function) {
        //在父表中记录本函数
        MTable funcTable = new MTable(curTable);
        new MFunctionSymbol(curTable, function.getMIPSName(), function.getParams().size(), funcTable);
        //初始化当前函数
        FrameStack frameStack = new FrameStack(8);
        MFunction mFunction = new MFunction(module, function.getMIPSName(), frameStack);
        if (function.isLeaf())
            mFunction.isLeaf = true;
        functionMap.put(function, mFunction);
        curTable = funcTable;
        curFunction = mFunction;
        registerManager.setAllTempRegFree();
        registerManager.setAllSavedRegFree();
        //使用引用计数分配全局寄存器
        //mFunction.getGlobalRegMap1(function.getRefCounter(), registerManager.getAllGlobalReg());
        //使用图着色分配全局寄存器
        getGlobalRegMap2(mFunction, function.getConflictGraph(), function.getRefCounter());

        //没有用到的全局寄存器可以随意分配（关掉更好）
//        Set<Register> set;
//        if(mFunction.getGlobalValueRegMap()!=null){
//            set = new HashSet<>(mFunction.getGlobalValueRegMap().values());
//        }else {
//            set = new HashSet<>();
//        }
//        System.out.println("debug "+set);
        //有可能引发bug！！！
        //registerManager.initGlobalAsSavedReg(set);
        //registerManager.initGlobal(set);

        boolean isFirstBlock = true;
        List<MBasicBlock> mBlocks = new ArrayList<>();
        //建立数据流图
        for (BasicBlock block : function.getBlocks()) {
            MBasicBlock mBasicBlock;
            if (isFirstBlock) {
                mBasicBlock = new MBasicBlock(mFunction, mFunction.getName(), block);
                isFirstBlock = false;
            } else {
                mBasicBlock = new MBasicBlock(mFunction, block.getMIPSLabel(), block);
            }
            mBlocks.add(mBasicBlock);
            //block2BlockMap.put(mBasicBlock, block);
            //会缺少lastBlock的映射，但是好像问题不大
            blockMap.put(mBasicBlock.getName(), mBasicBlock);
        }

        //翻译指令
        for (int index = 0; index < function.getBlocks().size(); index++) {
            BasicBlock block = function.getBlocks().get(index);
            MBasicBlock mBasicBlock = mBlocks.get(index);
//            if(curFunction.getGlobalValueRegMap()!=null){
//                //和下面的二选一
//                //registerManager.setAllGlobalRegFree();
//
//                //有可能引发bug
//                //registerManager.initGlobal(blockLiveGlobalRegs.get(mBasicBlock));
//            }
            //得到本块的活跃全局寄存器
            getBlockLiveGlobalRegs(block, mBasicBlock);
            List<Instruction> instList = block.getInstList();
            //第一个块一定是传参
            if (index == 0) {
                if(function.getName().equals("@main"))
                {
                    new IInst(mBasicBlock, IInst.Operation.ADDIU, Register.R29, Register.R29, frameStack.getSize(1));
                    new IInst(mBasicBlock, IInst.Operation.MOVE, Register.R30, Register.R29, null);
                }else {
                    new IInst(mBasicBlock, IInst.Operation.ADDIU, Register.R29, Register.R29, frameStack.getSize(1));
                    if (!function.isLeaf())
                        new IInst(mBasicBlock, IInst.Operation.SW, Register.R29, Register.R31, frameStack.getReg(1));
                    new IInst(mBasicBlock, IInst.Operation.SW, Register.R29, Register.R30, frameStack.getReg(0));
                    new IInst(mBasicBlock, IInst.Operation.MOVE, Register.R30, Register.R29, null);

                    //传入的参数单独处理
                    int paramSize = function.getParams().size();
                    for (int i = 0; i < paramSize * 2; i++) {
                        if (instList.get(i) instanceof AllocaInst inst) {
                            Value param = inst.getResult();
                            curFunction.addParam(param);
                            Register globalReg = curFunction.getGlobalReg(param);
                            if(globalReg!=null){
                                System.out.println(function.getName() +" " +param+" get "+globalReg);
                                new MVarSymbol(curTable, param.getName(), globalReg);
                            }
                            else {
                                //System.out.println(function.getName() +" load "+param);
                                if (inst.getResType().dePointer().isPointer()) {
                                    MemAddr addr = frameStack.getParamInAddr(i / 2, true);
                                    new MVarSymbol(curTable, inst.getResult().getName(), addr);
                                } else {
                                    MemAddr addr = frameStack.getParamInAddr(i / 2, false);
                                    new MVarSymbol(curTable, inst.getResult().getName(), addr);
                                }
                            }
                        }
                    }
                    instList = instList.subList(paramSize * 2, instList.size());
                }
            }
            for (Instruction inst : instList) {
                translateInst(mBasicBlock, inst);
            }
            translateInst(mBasicBlock, block.getEndInst());

        }
        curTable = funcTable.getParent();
        MBasicBlock oriLast = curFunction.getBLockList().get(mFunction.getBLockList().size()-1);
        MBasicBlock lastBlock = new MBasicBlock(mFunction, "zzq_exit_" + function.getMIPSName(), null);
        oriLast.addSuccessors(lastBlock);
        //检查是否有寄存器泄漏
        //registerManager.printAllSavedRegs();
        //返回
        if (function.getName().equals("@main")) {
            new IInst(lastBlock, IInst.Operation.LI, Register.R2, null, new ImmediateNumber("10"));
            new Syscall(lastBlock);
        } else {
            new IInst(lastBlock, IInst.Operation.MOVE, Register.R29, Register.R30, null);
            if (!function.isLeaf())
                new IInst(lastBlock, IInst.Operation.LW, Register.R29, Register.R31, frameStack.getReg(1));
            new IInst(lastBlock, IInst.Operation.LW, Register.R29, Register.R30, frameStack.getReg(0));
            new IInst(lastBlock, IInst.Operation.ADDIU, Register.R29, Register.R29, frameStack.getSize(0));
            new JInst(lastBlock, JInst.Operation.JR, Register.R31);
        }
    }

    private void translateInst(MBasicBlock mBasicBlock, Instruction instruction) {
//        if(instruction!=null)
//            new MComment(mBasicBlock, instruction);
        //System.out.println("Translate: " + instruction);
        if (instruction instanceof AllocaInst inst) {
            FrameStack fs = curFunction.getFrameStack();
            ValueType type = inst.getResType().dePointer();
            //普通变量
            if (type.isPointer() || !type.isArray()) {
                saveGlobalVal(inst.getResult());
                //new MVarSymbol(curTable, inst.getResult().getName(), fs.allocTemp(1, false));
            }
            //数组
            else {
                int words = 1;
                for (Integer i : type.getLens())
                    words *= i;
                new MVarSymbol(curTable, inst.getResult().getName(), fs.allocTemp(words, false));
            }
        }
        else if (instruction instanceof StoreInst inst) {
            Value lVal = inst.lVar;
            Value rVal = inst.rVar;
            Register rt;
            //右值为常数
            if (lVal instanceof ConstVal constVal) {
                if (constVal.getValue().equals("0")) {
                    rt = Register.R0;
                } else {
                    rt = getFreeTempReg();
                    new IInst(mBasicBlock, IInst.Operation.LI, rt, null, new ImmediateNumber(constVal.getValue()));
                }
                //rt为某处存放的值
            } else {
                MVarSymbol rtSymbol = (MVarSymbol) curTable.findSymbolInAll(lVal.getName());
                rt = loadSymbolValue(mBasicBlock, rtSymbol);
            }
            //rs一定是个内存地址 TODO 如果不是？
            MVarSymbol rsSymbol = (MVarSymbol) curTable.findSymbolInAll(rVal.getName());
            MValue rsv = rsSymbol.getValue();
            if (rsv instanceof MemAddr rs) {
                if (rs.isContainPointer()) {
                    System.out.println("StoreInst: 啊啊啊啊啊！");
                } else {
                    if (!(rs.getBase() instanceof Register)) {
                        Register temp = getFreeTempReg();
                        new IInst(mBasicBlock, IInst.Operation.LA, temp, rs.getBase(), null);
                        new IInst(mBasicBlock, IInst.Operation.SW, temp, rt, rs.getOffset());
                        setTSRegFree(temp);
                    } else {
                        new IInst(mBasicBlock, IInst.Operation.SW, rs.getBase(), rt, rs.getOffset());
                    }
                }
                setTSRegFree(rt);
                setTSRegFree(rs.getBase());
            } else {
                new IInst(mBasicBlock, IInst.Operation.MOVE, rsv, rt, null);
                setTSRegFree(rt);
            }
        }
        else if (instruction instanceof LoadInst inst) {
            MVarSymbol symbol = (MVarSymbol) curTable.findSymbolInAll(inst.source.getName());
            MValue source = symbol.getValue();
            if (source instanceof MemAddr addr) {
                //rs一定是地址，rt大多数情况下是寄存器，但是参数数组时会是地址
                MValue rt;
                if (inst.getResult().getType().isPointer()) {
                    rt = addr;
                    new MVarSymbol(curTable, inst.getResult().getName(), rt);
                } else {
                    rt = getFreeSavedReg();
                    if (rt == null)
                        rt = getFreeTempReg();
                    if (!(addr.getBase() instanceof Register)) {
                        //全局变量
                        new IInst(mBasicBlock, IInst.Operation.LA, rt, addr.getBase(), null);
                        new IInst(mBasicBlock, IInst.Operation.LW, rt, rt, addr.getOffset());
                    } else {
                        //存在栈中的变量
                        new IInst(mBasicBlock, IInst.Operation.LW, addr.getBase(), rt, addr.getOffset());
                    }
                    MValue save = saveTempReg(mBasicBlock, (Register) rt);
                    new MVarSymbol(curTable, inst.getResult().getName(), save);
                }
                setTSRegFree(addr.getBase());
            } else {
                MValue rt;
                if (inst.getResult().getType().isPointer()) {
                    rt = source;
                    new MVarSymbol(curTable, inst.getResult().getName(), rt);
                } else {
                    rt = getFreeSavedReg();
                    if (rt == null)
                        rt = getFreeTempReg();
                    //存在栈中的变量
                    new IInst(mBasicBlock, IInst.Operation.MOVE, rt, source, null);
                    MValue save = saveTempReg(mBasicBlock, (Register) rt);
                    new MVarSymbol(curTable, inst.getResult().getName(), save);
                }
            }
        } else if (instruction instanceof GetElementPtrInst inst) {
            //一定是个地址
            MVarSymbol baseSymbol = (MVarSymbol) curTable.findSymbolInAll(inst.source.getName());
            MValue value = baseSymbol.getValue();
            //MemAddr addr = (MemAddr) baseSymbol.getValue();
            MemAddr addr;
            if (value instanceof MemAddr a) {
                addr = a;
            } else {
                addr = new MemAddr(value, new ImmediateNumber("0"));
            }

            List<Integer> arraysize = inst.source.getType().getLens();
            int size = 1;
            for (Integer i : arraysize)
                size *= i;
            int sizeStore = size;
            List<Value> indexs = inst.indexs;
            int offset = 0;
            for (int i = 0; i < indexs.size(); i++) {
                Value val = indexs.get(i);
                if (i > 0)
                    size /= arraysize.get(i - 1);
                if (val instanceof ConstVal cv) {
                    offset += Integer.parseInt(cv.getValue()) * size;
                }
            }
            size = sizeStore;
            Register lastReg = null;
            for (int i = 0; i < indexs.size(); i++) {
                Value val = indexs.get(i);
                if (i > 0)
                    size /= arraysize.get(i - 1);
                if (!(val instanceof ConstVal)) {
                    MVarSymbol tempSymbol = (MVarSymbol) curTable.findSymbolInAll(val.getName());
                    //假定一定是reg
                    Register reg = loadSymbolValue(mBasicBlock, tempSymbol);
                    if (lastReg == null) {
                        Register iTemp = getFreeTempReg();
                        translateMul(mBasicBlock, reg, String.valueOf(4 * size));
                        setTSRegFree(iTemp);
                        lastReg = reg;
                    } else {
                        Register iTemp = getFreeTempReg();
                        translateMul(mBasicBlock, reg, String.valueOf(4 * size));
                        new RInst(mBasicBlock, RInst.Operation.ADDU, reg, reg, lastReg);
                        setTSRegFree(iTemp);
                        setTSRegFree(lastReg);
                        lastReg = reg;
                    }
                }
            }
            if (lastReg != null) {
                new IInst(mBasicBlock, IInst.Operation.ADDIU, lastReg, lastReg, new ImmediateNumber(String.valueOf(4 * offset)));
            }
            if (addr.isContainPointer()) {
                Register temp = getFreeSavedReg();
                if (temp == null)
                    temp = getFreeTempReg();
                new IInst(mBasicBlock, IInst.Operation.LW, addr.getBase(), temp, addr.getOffset());
                if (lastReg != null) {
                    new RInst(mBasicBlock, RInst.Operation.ADDU, lastReg, temp, lastReg);
                    new MVarSymbol(curTable, inst.getResult().getName(), new MemAddr(lastReg, new ImmediateNumber("0")));
                    setTSRegFree(temp);
                } else {
                    MemAddr save = saveTempReg(mBasicBlock, temp, new ImmediateNumber(String.valueOf(4 * offset)));
                    new MVarSymbol(curTable, inst.getResult().getName(), save);
                }
                setTSRegFree(addr.getBase());
            } else {
                if (lastReg != null) {
                    Register temp = getFreeTempReg();
                    if (!(addr.getBase() instanceof Register)) {
                        new IInst(mBasicBlock, IInst.Operation.LA, temp, addr.getBase(), null);
                        if (addr.getOffset() instanceof ImmediateNumber n && n.getValue().equals("0"))
                            temp = temp;
                        else
                            new IInst(mBasicBlock, IInst.Operation.ADDIU, temp, temp, addr.getOffset());
                    } else {
                        if (addr.getOffset() instanceof ImmediateNumber n && n.getValue().equals("0"))
                            temp = (Register) addr.getBase();
                        else
                            new IInst(mBasicBlock, IInst.Operation.ADDIU, temp, addr.getBase(), addr.getOffset());
                    }
                    new RInst(mBasicBlock, RInst.Operation.ADDU, lastReg, lastReg, temp);
                    setTSRegFree(temp);
                    new MVarSymbol(curTable, inst.getResult().getName(), new MemAddr(lastReg, new ImmediateNumber("0")));
                    setTSRegFree(addr.getBase());
                } else {
                    new MVarSymbol(curTable, inst.getResult().getName(), addr.getOffsetAddr(offset));
                }
            }
        } else if (instruction instanceof CallInst inst) {
            String funcName = inst.function.getName().substring(1);
            List<Value> params = inst.params;
            switch (funcName) {
                case "putch" -> {
                    new IInst(mBasicBlock, IInst.Operation.LI, Register.R2, null, new ImmediateNumber("11"));
                    ConstVal c = (ConstVal) params.get(0);
                    new IInst(mBasicBlock, IInst.Operation.LI, Register.R4, null, new ImmediateNumber(c.getValue()));
                    new Syscall(mBasicBlock);
                }
                case "getint" -> {
                    new IInst(mBasicBlock, IInst.Operation.LI, Register.R2, null, new ImmediateNumber("5"));
                    new Syscall(mBasicBlock);
                    new MVarSymbol(curTable, inst.getResult().getName(), Register.R2);
                }
                case "putint" -> {
                    new IInst(mBasicBlock, IInst.Operation.LI, Register.R2, null, new ImmediateNumber("1"));
                    Value v = params.get(0);
                    if (v instanceof ConstVal c) {
                        new IInst(mBasicBlock, IInst.Operation.LI, Register.R4, null, new ImmediateNumber(c.getValue()));
                    } else {
                        MVarSymbol symbol = (MVarSymbol) curTable.findSymbolInAll(v.getName());
                        Register t = loadSymbolValue(mBasicBlock, symbol);
                        new IInst(mBasicBlock, IInst.Operation.MOVE, Register.R4, t, null);
                        setTSRegFree(t);
                    }
                    new Syscall(mBasicBlock);
                }
                default -> {
                    List<MemAddr> paramMem = mBasicBlock.getFunction().getFrameStack().allocParam(params.size());
                    //传递形参
                    //优化：如果形参进入后会保存在全局寄存器的话，那其实没必要存进栈
                    MFunction call = functionMap.get(inst.function);
                    Map<Value, Register> map = call.getGlobalValueRegMap();

                    //step0:后面可能会用到全局寄存器，找到需要的并保存
                    Map<Register, Register> tmpSet = new HashMap<>();
                    for(int i = 0; i<params.size(); i++) {
                        Value param = params.get(i);
                        if(!(param instanceof ConstVal)){
                            String name = param.getName();
                            MVarSymbol symbol = (MVarSymbol) curTable.findSymbolInAll(name);
                            MValue value = symbol.getValue();
                            if(value instanceof Register reg&& reg.isMyGlobal()){
                                //当返回null时可能有bug
                                Register t = getFreeSavedReg();
                                tmpSet.put(reg, t);
                                new IInst(mBasicBlock, IInst.Operation.MOVE, t, reg, null);
                            }
                        }
                    }
                    //step1:保存全局寄存器
                    List<Register> list = new ArrayList<>();
                    //list.addAll(registerManager.getOccupiedGlobalRegs());
                    if(blockLiveGlobalRegs.get(mBasicBlock)!=null)
                        list.addAll(blockLiveGlobalRegs.get(mBasicBlock));
                    //如果调用的函数是叶子函数 且 用不到某些全局寄存器，就不需要保存了
                    if(call.isLeaf){
                        Set<Register> usedGlobals;
                        if(call.getGlobalValueRegMap()!=null){
                            usedGlobals = new HashSet<>(call.getGlobalValueRegMap().values());
                        }else {
                            usedGlobals = new HashSet<>();
                        }
                        for(int i = 0;i<list.size();i++){
                            Register reg = list.get(i);
                            if(!usedGlobals.contains(reg))
                                list.remove(reg);
                        }
                    }
                    System.out.println("save global : "+list);
                    registerManager.saveGlobalStatus();
                    Map<Register, MemAddr> addrs = new HashMap<>();
                    for(Register reg : list){
                        addrs.put(reg, saveRegsForCall(mBasicBlock, reg));
                    }
                    list.clear();
                    System.out.println(curFunction.getName()+ " call "+inst.function.getName());
                    //System.out.println("debug1: "+map);
                    List<Value> paramsRef = call.getParams();
                    //System.out.println("debug2: "+paramsRef);
                    //step2:对于需要存进全局寄存器的参数，直接存入寄存器；如果不需要，就老实存进栈
                    for(int i = 0; i<params.size(); i++){
                        Value param = params.get(i);
                        if(map!=null && map.containsKey(paramsRef.get(i))){
                            //System.out.println(call.getName() + " pass "+paramsRef.get(i));
                            Register save = map.get(paramsRef.get(i));
                            //如果是常量
                            if(param instanceof ConstVal c){
                                new IInst(mBasicBlock, IInst.Operation.LI, save, null, new ImmediateNumber(c.getValue()));
                            }
                            else {
                                String name = param.getName();
                                MVarSymbol symbol = (MVarSymbol) curTable.findSymbolInAll(name);
                                if(param.getType().isPointer()){
                                    //如果传递的是数组
                                    MValue value = symbol.getValue();
                                    MemAddr addr;
                                    if (value instanceof MemAddr ma) {
                                        addr = ma;
                                    } else {
                                        if(tmpSet.containsKey(value))
                                            value = tmpSet.get(value);
                                        addr = new MemAddr(value, new ImmediateNumber("0"));
                                    }
                                    if (addr.isContainPointer()) {
                                        //System.out.println("1");
                                        new IInst(mBasicBlock, IInst.Operation.LW, addr.getBase(), save, addr.getOffset());
                                    } else if (!(addr.getBase() instanceof Register)) {
                                        //System.out.println("2");
                                        new IInst(mBasicBlock, IInst.Operation.LA, save, addr.getBase(), null);
                                        if (addr.getOffset() instanceof ImmediateNumber n && n.getValue().equals("0"))
                                            save = save;
                                        else
                                            new IInst(mBasicBlock, IInst.Operation.ADDIU, save, save, addr.getOffset());
                                    } else {
                                        //System.out.println("3");
                                        MInstruction in = new IInst(mBasicBlock, IInst.Operation.ADDIU, save, addr.getBase(), addr.getOffset());
                                        //System.out.println(in);
                                    }
                                    //System.out.println("4");
                                    setTSRegFree(addr.getBase());
                                }else {
                                    //如果传递的是普通变量
                                    Register reg = loadSymbolValue(mBasicBlock, symbol);
                                    if(tmpSet.containsKey(reg))
                                        reg = tmpSet.get(reg);
                                    new IInst(mBasicBlock, IInst.Operation.MOVE, save, reg, null);
                                    setTSRegFree(reg);
                                }
                            }
                        }
                        else {
                            //System.out.println(call.getName() +" store "+paramsRef.get(i));
                            //System.out.println(param);
                            if (param instanceof ConstVal cVal) {
                                Register reg = getFreeTempReg();
                                new IInst(mBasicBlock, IInst.Operation.LI, reg, null, new ImmediateNumber(cVal.getValue()));
                                new IInst(mBasicBlock, IInst.Operation.SW, paramMem.get(i).getBase(), reg, paramMem.get(i).getOffset());
                                setTSRegFree(reg);
                            }
                            else {
                                String name = param.getName();
                                MVarSymbol symbol = (MVarSymbol) curTable.findSymbolInAll(name);
                                if (param.getType().isPointer()) {
                                    //传递指针
                                    //用add算出来最终地址
                                    //然后存在对应位置
                                    Register temp = getFreeTempReg();
                                    MValue value = symbol.getValue();
                                    //MemAddr addr = (MemAddr) symbol.getValue();
                                    MemAddr addr;
                                    if (value instanceof MemAddr ma) {
                                        addr = ma;
                                    } else {
                                        addr = new MemAddr(value, new ImmediateNumber("0"));
                                    }
                                    if (addr.isContainPointer()) {
                                        new IInst(mBasicBlock, IInst.Operation.LW, addr.getBase(), temp, addr.getOffset());
                                    } else if (!(addr.getBase() instanceof Register)) {
                                        new IInst(mBasicBlock, IInst.Operation.LA, temp, addr.getBase(), null);
                                        if (addr.getOffset() instanceof ImmediateNumber n && n.getValue().equals("0"))
                                            temp = temp;
                                        else
                                            new IInst(mBasicBlock, IInst.Operation.ADDIU, temp, temp, addr.getOffset());
                                    } else {
                                        if (addr.getOffset() instanceof ImmediateNumber n && n.getValue().equals("0"))
                                            temp = (Register) addr.getBase();
                                        else
                                            new IInst(mBasicBlock, IInst.Operation.ADDIU, temp, addr.getBase(), addr.getOffset());
                                    }
                                    new IInst(mBasicBlock, IInst.Operation.SW, paramMem.get(i).getBase(), temp, paramMem.get(i).getOffset());
                                    setTSRegFree(temp);
                                    setTSRegFree(addr.getBase());
                                }
                                else {
                                    Register reg = loadSymbolValue(mBasicBlock, symbol);
                                    setTSRegFree(reg);
                                    new IInst(mBasicBlock, IInst.Operation.SW, paramMem.get(i).getBase(), reg, paramMem.get(i).getOffset());
                                }
                            }
                        }
                    }

                    //step4:保存局部寄存器
                    list.addAll(registerManager.getOccupiedSavedRegs());
                    for(Register reg : list){
                        addrs.put(reg, saveRegsForCall(mBasicBlock, reg));
                    }
                    registerManager.saveSavedStatus();
//                    //System.out.println("debug: " + funcName + " " + list);
//                    if (funcRegs.containsKey(funcName)) {
//                        Set<Register> oriSet = funcRegs.get(funcName);
//                        oriSet.addAll(list);
//                    } else {
//                        Set<Register> regSet = new HashSet<>(list);
//                        funcRegs.put(funcName, regSet);
//                    }
                    //跳转
                    new JInst(mBasicBlock, JInst.Operation.JAL, new Label(funcName));

                    //恢复保存的寄存器
                    for(Map.Entry<Register, MemAddr> entry : addrs.entrySet()){
                        Register reg = entry.getKey();
                        MemAddr mem = entry.getValue();
                        new IInst(mBasicBlock, IInst.Operation.LW, mem.getBase(), reg, mem.getOffset());
                    }
                    registerManager.recoverStatus();
                    //如果没用的的话就不保存了！
                    if (inst.getResult() != null) {
                        if(mBasicBlock.getIRblock().getOut()==null || mBasicBlock.getIRblock().getOut().contains(inst.getResult())){
                            MValue save = saveTempReg(mBasicBlock, Register.R2);
                            new MVarSymbol(curTable, inst.getResult().getName(), save);
                        }
                    }
                    //System.out.println("call "+funcName+" in "+curFunction.getName());
                    //printAllSavedReg();
                    //System.out.println("==================");
                }
            }
        } else if (instruction instanceof BrInst inst) {
            if (inst.isCond()) {
                String desName1 = inst.b1.getMIPSLabel();
                Label des1 = new Label(desName1);
                String desName2 = inst.b2.getMIPSLabel();
                Label des2 = new Label(desName2);
                Value cond = inst.cond;
                if (cond instanceof ConstVal c) {
                    if (c.getValue().equals("true")) {
                        new JInst(mBasicBlock, JInst.Operation.J, des1);
                        MBasicBlock desBlock1 = blockMap.get(des1.toString());
                        mBasicBlock.addSuccessors(desBlock1);
                        desBlock1.addPredecessor(mBasicBlock);

                    } else {
                        new JInst(mBasicBlock, JInst.Operation.J, des2);
                        MBasicBlock desBlock2 = blockMap.get(des2.toString());
                        mBasicBlock.addSuccessors(desBlock2);
                        desBlock2.addPredecessor(mBasicBlock);
                    }
                } else {
                    String condName = inst.cond.getName();
                    MVarSymbol condSymbol = (MVarSymbol) curTable.findSymbolInAll(condName);
                    Register reg = loadSymbolValue(mBasicBlock, condSymbol);
                    new RInst(mBasicBlock, RInst.Operation.BEQ, reg, Register.R0, des2);
                    //new IInst(mBasicBlock, IInst.Operation.BNEZ, reg,des1, null);
                    new JInst(mBasicBlock, JInst.Operation.J, des1);
                    MBasicBlock desBlock1 = blockMap.get(des1.toString());
                    MBasicBlock desBlock2 = blockMap.get(des2.toString());
                    mBasicBlock.addSuccessors(desBlock1);
                    mBasicBlock.addSuccessors(desBlock2);
                    desBlock1.addPredecessor(mBasicBlock);
                    desBlock2.addPredecessor(mBasicBlock);
                    setTSRegFree(reg);
                }
            } else {
                String desName = inst.b1.getMIPSLabel();
                Label des = new Label(desName);
                new JInst(mBasicBlock, JInst.Operation.J, des);
                MBasicBlock desBlock = blockMap.get(des.toString());
                mBasicBlock.addSuccessors(desBlock);
                desBlock.addPredecessor(mBasicBlock);
            }
        } else if (instruction instanceof IcmpInst inst) {
            Value lv = inst.lVar, rv = inst.rVar;
            Tokens.TokenKind op = inst.operation;
            Register lReg, rReg;
            if (lv instanceof ConstVal c) {
                if (c.getValue().equals("0"))
                    lReg = Register.R0;
                else {
                    Register constTemp = getFreeTempReg();
                    new IInst(mBasicBlock, IInst.Operation.LI, constTemp, null, new ImmediateNumber(c.getValue()));
                    lReg = constTemp;
                }
            } else {
                MVarSymbol lVar = (MVarSymbol) curTable.findSymbolInAll(lv.getName());
                lReg = loadSymbolValue(mBasicBlock, lVar);
            }
            if (rv instanceof ConstVal c) {
                if (c.getValue().equals("0")) {
                    rReg = Register.R0;
                } else {
                    Register constTemp = getFreeTempReg();
                    new IInst(mBasicBlock, IInst.Operation.LI, constTemp, null, new ImmediateNumber(c.getValue()));
                    rReg = constTemp;
                }
            } else {
                MVarSymbol rVar = (MVarSymbol) curTable.findSymbolInAll(rv.getName());
                rReg = loadSymbolValue(mBasicBlock, rVar);
            }

            Register res = getFreeSavedReg();
            if (res == null)
                res = getFreeTempReg();
            switch (op) {
                case GRE -> {
                    new RInst(mBasicBlock, RInst.Operation.SLT, res, rReg, lReg);
                }
                case LSS -> {
                    new RInst(mBasicBlock, RInst.Operation.SLT, res, lReg, rReg);
                }
                case GEQ -> {
                    new RInst(mBasicBlock, RInst.Operation.SLT, res, lReg, rReg);
                    new IInst(mBasicBlock, IInst.Operation.XORI, res, res, new ImmediateNumber("1"));
                }
                case LEQ -> {
                    new RInst(mBasicBlock, RInst.Operation.SLT, res, rReg, lReg);
                    new IInst(mBasicBlock, IInst.Operation.XORI, res, res, new ImmediateNumber("1"));
                }
                case EQL -> {
                    Register t1 = getFreeTempReg();
                    new RInst(mBasicBlock, RInst.Operation.XOR, t1, lReg, rReg);
                    new IInst(mBasicBlock, IInst.Operation.SLTIU, res, t1, new ImmediateNumber("1"));
                    setTSRegFree(t1);
                }
                case NEQ -> {
                    new RInst(mBasicBlock, RInst.Operation.SUBU, res, lReg, rReg);
                }
            }
            MValue save = saveTempReg(mBasicBlock, res);
            new MVarSymbol(curTable, inst.getResult().getName(), save);
            setTSRegFree(lReg);
            setTSRegFree(rReg);
        } else if (instruction instanceof RetInst inst) {
            if (!inst.retType.isType(ValueType.IRType.VOID, 0)) {
                if (inst.retVal instanceof ConstVal c) {
                    ImmediateNumber imm = new ImmediateNumber(c.getValue());
                    new IInst(mBasicBlock, IInst.Operation.LI, Register.R2, null, imm);
                } else {
                    MVarSymbol ret = (MVarSymbol) curTable.findSymbolInAll(inst.retVal.getName());
                    Register retReg = loadSymbolValue(mBasicBlock, ret);
                    new IInst(mBasicBlock, IInst.Operation.MOVE, Register.R2, retReg, null);
                    setTSRegFree(retReg);
                }
            }
            new JInst(mBasicBlock, JInst.Operation.J, new Label("zzq_exit_" + mBasicBlock.getFunction().getName()));
        } else if (instruction instanceof BinaryInst inst) {
            switch (inst.type) {
                case ADD -> translateAdd(mBasicBlock, inst);
                case SUB -> translateSub(mBasicBlock, inst);
                case MUL -> translateMul(mBasicBlock, inst);
                case SREM -> translateMod(mBasicBlock, inst);
                case SDIV -> translateDiv(mBasicBlock, inst);
            }
        } else if (instruction instanceof ZextInst inst) {
            Value source = inst.source;
            MValue reg;
            if (source instanceof ConstVal c) {
                reg = getFreeSavedReg();
                if (reg == null)
                    reg = getFreeTempReg();
                String res;
                if (c.getValue().equals("true"))
                    res = "1";
                else
                    res = "0";
                new IInst(mBasicBlock, IInst.Operation.LI, reg, null, new ImmediateNumber(res));
                reg = saveTempReg(mBasicBlock, (Register) reg);
            } else {
                MVarSymbol sourceSymbol = (MVarSymbol) curTable.findSymbolInAll(source.getName());
                reg = loadSymbolValue(mBasicBlock, sourceSymbol);
                new RInst(mBasicBlock, RInst.Operation.SLTU, reg, Register.R0, reg);
            }
            new MVarSymbol(curTable, inst.getResult().getName(), reg);
        } else if (instruction instanceof MoveInst inst) {
            Value source = inst.source;
            MVarSymbol sourceSymbol = (MVarSymbol) curTable.findSymbolInAll(source.getName());
            Register reg = loadSymbolValue(mBasicBlock, sourceSymbol);
            Register des = getFreeSavedReg();
            new IInst(mBasicBlock, IInst.Operation.MOVE, des, reg, null);
            new MVarSymbol(curTable, inst.getResult().getName(), des);
        }
    }

    private void translateAdd(MBasicBlock mBasicBlock, BinaryInst inst) {
        Value lv = inst.lVar;
        Value rv = inst.rVar;
        //System.out.println(lv);
        if (lv instanceof ConstVal c) {
            Value t = lv;
            lv = rv;
            rv = t;
        }
        MVarSymbol lSymbol = (MVarSymbol) curTable.findSymbolInAll(lv.getName());
        Register lReg = loadSymbolValue(mBasicBlock, lSymbol);
        if (rv instanceof ConstVal c) {
            int x = Integer.parseInt(c.getValue());
            if (x == 0) {
                new MVarSymbol(curTable, inst.getResult().getName(), lReg);
                return;
            }
            ImmediateNumber imm = new ImmediateNumber(c.getValue());
            new IInst(mBasicBlock, IInst.Operation.ADDIU, lReg, lReg, imm);
        } else {
            MVarSymbol rSymbol = (MVarSymbol) curTable.findSymbolInAll(rv.getName());
            Register rReg = loadSymbolValue(mBasicBlock, rSymbol);
            new RInst(mBasicBlock, RInst.Operation.ADDU, lReg, lReg, rReg);
            setTSRegFree(rReg);
        }
        MValue save = saveTempReg(mBasicBlock, lReg);
        new MVarSymbol(curTable, inst.getResult().getName(), save);
    }

    private void translateBinaryR(MBasicBlock mBasicBlock, BinaryInst inst) {
        Value lv = inst.lVar;
        Value rv = inst.rVar;
        Register lReg, rReg;
        if (lv instanceof ConstVal) {
            Register temp = getFreeSavedReg();
            if (temp == null)
                temp = getFreeTempReg();
            ImmediateNumber imm = new ImmediateNumber(((ConstVal) lv).getValue());
            new IInst(mBasicBlock, IInst.Operation.LI, temp, null, imm);
            lReg = temp;
        } else {
            MVarSymbol lSymbol = (MVarSymbol) curTable.findSymbolInAll(lv.getName());
            lReg = loadSymbolValue(mBasicBlock, lSymbol);
        }
        if (rv instanceof ConstVal) {
            Register temp = getFreeTempReg();
            ImmediateNumber imm = new ImmediateNumber(((ConstVal) rv).getValue());
            new IInst(mBasicBlock, IInst.Operation.LI, temp, null, imm);
            rReg = temp;
        } else {
            MVarSymbol rSymbol = (MVarSymbol) curTable.findSymbolInAll(rv.getName());
            rReg = loadSymbolValue(mBasicBlock, rSymbol);
        }

        switch (inst.type) {
//            case SUB -> new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, lReg, rReg);
//            case MUL -> new RInst(mBasicBlock, RInst.Operation.MUL, lReg, lReg, rReg);
//            case SREM -> new RInst(mBasicBlock, RInst.Operation.REM, lReg, lReg, rReg);
//            case SDIV -> {
//                new RInst(mBasicBlock, RInst.Operation.DIV, lReg, lReg, rReg);
//                new RInst(mBasicBlock, RInst.Operation.MFLO, lReg, null, null);
//            }
        }
        MValue save = saveTempReg(mBasicBlock, lReg);
        new MVarSymbol(curTable, inst.getResult().getName(), save);
        setTSRegFree(rReg);
    }

    private void translateSub(MBasicBlock mBasicBlock, BinaryInst inst) {
        Value lv = inst.lVar;
        Value rv = inst.rVar;
        Register lReg, rReg;
        if (lv instanceof ConstVal c) {
            int x = Integer.parseInt(c.getValue());
            if (x == 0) {
                //提前取rReg
                MVarSymbol rSymbol = (MVarSymbol) curTable.findSymbolInAll(rv.getName());
                rReg = loadSymbolValue(mBasicBlock, rSymbol);
                new RInst(mBasicBlock, RInst.Operation.SUBU, rReg, Register.R0, rReg);
                MValue save = saveTempReg(mBasicBlock, rReg);
                new MVarSymbol(curTable, inst.getResult().getName(), save);
                return;
            }
            Register temp = getFreeSavedReg();
            if (temp == null)
                temp = getFreeTempReg();
            ImmediateNumber imm = new ImmediateNumber(c.getValue());
            new IInst(mBasicBlock, IInst.Operation.LI, temp, null, imm);
            lReg = temp;
        } else {
            MVarSymbol lSymbol = (MVarSymbol) curTable.findSymbolInAll(lv.getName());
            lReg = loadSymbolValue(mBasicBlock, lSymbol);
        }
        if (rv instanceof ConstVal) {
            //如果rv是常数，说明lv一定不是常数
            int negRv = -Integer.parseInt(((ConstVal) rv).getValue());
            if (negRv == 0) {
                MValue save = saveTempReg(mBasicBlock, lReg);
                new MVarSymbol(curTable, inst.getResult().getName(), save);
                return;
            }
            ImmediateNumber imm = new ImmediateNumber(String.valueOf(negRv));
            new IInst(mBasicBlock, IInst.Operation.ADDIU, lReg, lReg, imm);
            MValue save = saveTempReg(mBasicBlock, lReg);
            new MVarSymbol(curTable, inst.getResult().getName(), save);
            return;
        } else {
            MVarSymbol rSymbol = (MVarSymbol) curTable.findSymbolInAll(rv.getName());
            rReg = loadSymbolValue(mBasicBlock, rSymbol);
        }
        new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, lReg, rReg);
        MValue save = saveTempReg(mBasicBlock, lReg);
        new MVarSymbol(curTable, inst.getResult().getName(), save);
        setTSRegFree(rReg);
    }

    private void translateMul(MBasicBlock mBasicBlock, BinaryInst inst) {
        Value lv = inst.lVar;
        Value rv = inst.rVar;
        Register lReg, rReg;
        if (lv instanceof ConstVal) {
            Value temp = lv;
            lv = rv;
            rv = temp;
        }
        MVarSymbol lSymbol = (MVarSymbol) curTable.findSymbolInAll(lv.getName());
        lReg = loadSymbolValue(mBasicBlock, lSymbol);
        if (rv instanceof ConstVal c) {
            int x = Integer.parseInt(c.getValue());
            int abs = x > 0 ? x : -x;
            int k;
            if (abs == 0) {
                new MVarSymbol(curTable, inst.getResult().getName(), Register.R0);
                setTSRegFree(lReg);
                return;
            } else if (abs == 1) {
                //the same
            } else {
                if (isPowerOfTwo(abs)) {
                    k = Integer.toBinaryString(abs).length() - 1;
                    new IInst(mBasicBlock, IInst.Operation.SLL, lReg, lReg, new ImmediateNumber(String.valueOf(k)));
                } else if (isPowerOfTwoPlusOne(abs)) {
                    abs -= 1;
                    k = Integer.toBinaryString(abs).length() - 1;
                    Register tempReg = getFreeTempReg();
                    new IInst(mBasicBlock, IInst.Operation.SLL, tempReg, lReg, new ImmediateNumber(String.valueOf(k)));
                    new RInst(mBasicBlock, RInst.Operation.ADDU, lReg, lReg, tempReg);
                    setTSRegFree(tempReg);
                } else if (isPowerOfTwoMinusOne(abs)) {
                    abs += 1;
                    k = Integer.toBinaryString(abs).length() - 1;
                    Register tempReg = getFreeTempReg();
                    new IInst(mBasicBlock, IInst.Operation.SLL, tempReg, lReg, new ImmediateNumber(String.valueOf(k)));
                    new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, tempReg, lReg);
                    setTSRegFree(tempReg);
                } else {
                    Register temp = getFreeTempReg();
                    ImmediateNumber imm = new ImmediateNumber(((ConstVal) rv).getValue());
                    new IInst(mBasicBlock, IInst.Operation.LI, temp, null, imm);
                    rReg = temp;
                    new RInst(mBasicBlock, RInst.Operation.MUL, lReg, lReg, rReg);
                    MValue save = saveTempReg(mBasicBlock, lReg);
                    new MVarSymbol(curTable, inst.getResult().getName(), save);
                    setTSRegFree(rReg);
                    return;
                }
            }
            if (x < 0) {
                new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, Register.R0, lReg);
            }
            MValue save = saveTempReg(mBasicBlock, lReg);
            new MVarSymbol(curTable, inst.getResult().getName(), save);
        } else {
            MVarSymbol rSymbol = (MVarSymbol) curTable.findSymbolInAll(rv.getName());
            rReg = loadSymbolValue(mBasicBlock, rSymbol);
            new RInst(mBasicBlock, RInst.Operation.MUL, lReg, lReg, rReg);
            MValue save = saveTempReg(mBasicBlock, lReg);
            new MVarSymbol(curTable, inst.getResult().getName(), save);
            setTSRegFree(rReg);
        }
    }

    private void translateMul(MBasicBlock mBasicBlock, Register lReg, String c) {
        Register rReg;
        int x = Integer.parseInt(c);
        int abs = x > 0 ? x : -x;
        int k;
        if (abs == 0) {
            new IInst(mBasicBlock, IInst.Operation.MOVE, lReg, Register.R0, null);
            return;
        } else if (abs == 1) {
            //the same
        } else {
            if (isPowerOfTwo(abs)) {
                k = Integer.toBinaryString(abs).length() - 1;
                new IInst(mBasicBlock, IInst.Operation.SLL, lReg, lReg, new ImmediateNumber(String.valueOf(k)));
            } else if (isPowerOfTwoPlusOne(abs)) {
                abs -= 1;
                k = Integer.toBinaryString(abs).length() - 1;
                Register tempReg = getFreeTempReg();
                new IInst(mBasicBlock, IInst.Operation.SLL, tempReg, lReg, new ImmediateNumber(String.valueOf(k)));
                new RInst(mBasicBlock, RInst.Operation.ADDU, lReg, lReg, tempReg);
                setTSRegFree(tempReg);
            } else if (isPowerOfTwoMinusOne(abs)) {
                abs += 1;
                k = Integer.toBinaryString(abs).length() - 1;
                Register tempReg = getFreeTempReg();
                new IInst(mBasicBlock, IInst.Operation.SLL, tempReg, lReg, new ImmediateNumber(String.valueOf(k)));
                new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, lReg, tempReg);
                setTSRegFree(tempReg);
            } else {
                Register temp = getFreeTempReg();
                new IInst(mBasicBlock, IInst.Operation.LI, temp, null, new ImmediateNumber(String.valueOf(c)));
                rReg = temp;
                new RInst(mBasicBlock, RInst.Operation.MUL, lReg, lReg, rReg);
                setTSRegFree(rReg);
                return;
            }
        }
        if (x < 0) {
            new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, Register.R0, lReg);
        }
    }

    private void translateDiv(MBasicBlock mBasicBlock, BinaryInst inst) {
        Value lv = inst.lVar;
        Value rv = inst.rVar;
        Register lReg, rReg;
        //三种情况：
        // 1.被除数是常数 2.除数是常数 3.都不是常数
        if (lv instanceof ConstVal c) {
            int x = Integer.parseInt(c.getValue());
            if (x == 0) {
                new MVarSymbol(curTable, inst.getResult().getName(), Register.R0);
                return;
            } else {
                Register temp = getFreeSavedReg();
                if (temp == null)
                    temp = getFreeTempReg();
                ImmediateNumber imm = new ImmediateNumber(((ConstVal) lv).getValue());
                new IInst(mBasicBlock, IInst.Operation.LI, temp, null, imm);
                lReg = temp;
            }
        } else if (rv instanceof ConstVal c) {
            MVarSymbol lSymbol = (MVarSymbol) curTable.findSymbolInAll(lv.getName());
            lReg = loadSymbolValue(mBasicBlock, lSymbol);
            int x = Integer.parseInt(c.getValue());
            if (x == 1) {
                new MVarSymbol(curTable, inst.getResult().getName(), lReg);
                return;
            } else if (x == -1) {
                new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, Register.R0, lReg);
                new MVarSymbol(curTable, inst.getResult().getName(), lReg);
                return;
            } else {
                int abs = x > 0 ? x : -x;
                //如果是2的整数次方
                if ((abs & (abs - 1)) == 0) {
                    //(n + ((n >> (31)) >>> (32 - l))) >> l
                    int l = Integer.toBinaryString(abs).length() - 1;
                    Register temp = getFreeTempReg();
                    new IInst(mBasicBlock, IInst.Operation.SRA, temp, lReg, new ImmediateNumber("31"));
                    new IInst(mBasicBlock, IInst.Operation.SRL, temp, temp, new ImmediateNumber(String.valueOf(32 - l)));
                    new RInst(mBasicBlock, RInst.Operation.ADDU, lReg, lReg, temp);
                    new IInst(mBasicBlock, IInst.Operation.SRA, lReg, lReg, new ImmediateNumber(String.valueOf(l)));
                    setTSRegFree(temp);
                } else {
                    //奇怪的运算
                    long nc = (1L << 31) - ((1L << 31) % abs) - 1;
                    long p = 32;
                    while ((1L << p) <= nc * (abs - (1L << p) % abs)) {
                        p++;
                    }
                    long n = (((1L << p) + (long) abs - (1L << p) % abs) / (long) abs);
                    long m = ((n << 32) >>> 32);
                    int sh = (int) (p - 32);
                    Register t1 = getFreeTempReg();
                    Register t2 = getFreeTempReg();
                    if (m < 2147483648L) {
                        new IInst(mBasicBlock, IInst.Operation.LI, t1, t1, new ImmediateNumber(String.valueOf(m)));
                        new IInst(mBasicBlock, IInst.Operation.MULT, lReg, t1, null);
                        new RInst(mBasicBlock, RInst.Operation.MFHI, t2, null, null);
                    } else {
                        new IInst(mBasicBlock, IInst.Operation.LI, t1, t1, new ImmediateNumber(String.valueOf(m - (1L << 32))));
                        new IInst(mBasicBlock, IInst.Operation.MULT, lReg, t1, null);
                        new RInst(mBasicBlock, RInst.Operation.MFHI, t2, null, null);
                        new RInst(mBasicBlock, RInst.Operation.ADDU, t2, t2, lReg);
                    }
                    new IInst(mBasicBlock, IInst.Operation.SRA, t2, t2, new ImmediateNumber(String.valueOf(sh)));
                    new IInst(mBasicBlock, IInst.Operation.SRL, t1, lReg, new ImmediateNumber(String.valueOf(31)));
                    new RInst(mBasicBlock, RInst.Operation.ADDU, lReg, t1, t2);
                    setTSRegFree(t1);
                    setTSRegFree(t2);
                }
                if (x < 0) {
                    new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, Register.R0, lReg);
                }
                new MVarSymbol(curTable, inst.getResult().getName(), lReg);
                return;
            }
        } else {
            MVarSymbol lSymbol = (MVarSymbol) curTable.findSymbolInAll(lv.getName());
            lReg = loadSymbolValue(mBasicBlock, lSymbol);
        }
        MVarSymbol rSymbol = (MVarSymbol) curTable.findSymbolInAll(rv.getName());
        rReg = loadSymbolValue(mBasicBlock, rSymbol);
        new RInst(mBasicBlock, RInst.Operation.DIV, lReg, lReg, rReg);
        new RInst(mBasicBlock, RInst.Operation.MFLO, lReg, null, null);
        MValue save = saveTempReg(mBasicBlock, lReg);
        new MVarSymbol(curTable, inst.getResult().getName(), save);
        setTSRegFree(rReg);
    }

    private void translateMod(MBasicBlock mBasicBlock, BinaryInst inst) {
        Value lv = inst.lVar;
        Value rv = inst.rVar;
        Register lReg, rReg;
        //三种情况：
        // 1.被模数是常数 2.模数是常数 3.都不是常数
        if (lv instanceof ConstVal c) {
            int x = Integer.parseInt(c.getValue());
            if (x == 0) {
                new MVarSymbol(curTable, inst.getResult().getName(), Register.R0);
                return;
            } else {
                Register temp = getFreeSavedReg();
                if (temp == null)
                    temp = getFreeTempReg();
                ImmediateNumber imm = new ImmediateNumber(((ConstVal) lv).getValue());
                new IInst(mBasicBlock, IInst.Operation.LI, temp, null, imm);
                lReg = temp;
            }
        } else if (rv instanceof ConstVal c) {
            int x = Integer.parseInt(c.getValue());
            if (x == 1 || x == -1) {
                new MVarSymbol(curTable, inst.getResult().getName(), Register.R0);
                return;
            }
            MVarSymbol lSymbol = (MVarSymbol) curTable.findSymbolInAll(lv.getName());
            lReg = loadSymbolValue(mBasicBlock, lSymbol);
            Register tmp = getFreeTempReg();
            if(x>0){
                new IInst(mBasicBlock, IInst.Operation.MOVE, tmp, lReg, null);
            }else {
                new RInst(mBasicBlock, RInst.Operation.SUBU, tmp, Register.R0, lReg);
            }
            // n % x == n - (n/x)*x
            //step1: n = n/x
            int abs = x > 0 ? x : -x;
            //如果是2的整数次方
            if ((abs & (abs - 1)) == 0) {
                //(n + ((n >> (31)) >>> (32 - l))) >> l
                int l = Integer.toBinaryString(abs).length() - 1;
                Register temp = getFreeTempReg();
                new IInst(mBasicBlock, IInst.Operation.SRA, temp, lReg, new ImmediateNumber("31"));
                new IInst(mBasicBlock, IInst.Operation.SRL, temp, temp, new ImmediateNumber(String.valueOf(32 - l)));
                new RInst(mBasicBlock, RInst.Operation.ADDU, lReg, lReg, temp);
                new IInst(mBasicBlock, IInst.Operation.SRA, lReg, lReg, new ImmediateNumber(String.valueOf(l)));
                setTSRegFree(temp);
            } else {
                //奇怪的运算
                long nc = (1L << 31) - ((1L << 31) % abs) - 1;
                long p = 32;
                while ((1L << p) <= nc * (abs - (1L << p) % abs)) {
                    p++;
                }
                long n = (((1L << p) + (long) abs - (1L << p) % abs) / (long) abs);
                long m = ((n << 32) >>> 32);
                int sh = (int) (p - 32);
                Register t1 = getFreeTempReg();
                Register t2 = getFreeTempReg();
                if (m < 2147483648L) {
                    new IInst(mBasicBlock, IInst.Operation.LI, t1, t1, new ImmediateNumber(String.valueOf(m)));
                    new IInst(mBasicBlock, IInst.Operation.MULT, lReg, t1, null);
                    new RInst(mBasicBlock, RInst.Operation.MFHI, t2, null, null);
                } else {
                    new IInst(mBasicBlock, IInst.Operation.LI, t1, t1, new ImmediateNumber(String.valueOf(m - (1L << 32))));
                    new IInst(mBasicBlock, IInst.Operation.MULT, lReg, t1, null);
                    new RInst(mBasicBlock, RInst.Operation.MFHI, t2, null, null);
                    new RInst(mBasicBlock, RInst.Operation.ADDU, t2, t2, lReg);
                }
                new IInst(mBasicBlock, IInst.Operation.SRA, t2, t2, new ImmediateNumber(String.valueOf(sh)));
                new IInst(mBasicBlock, IInst.Operation.SRL, t1, lReg, new ImmediateNumber(String.valueOf(31)));
                new RInst(mBasicBlock, RInst.Operation.ADDU, lReg, t1, t2);
                setTSRegFree(t1);
                setTSRegFree(t2);
            }
            //step2: n = n*x
            int k;
            if (isPowerOfTwo(abs)) {
                k = Integer.toBinaryString(abs).length() - 1;
                new IInst(mBasicBlock, IInst.Operation.SLL, lReg, lReg, new ImmediateNumber(String.valueOf(k)));
            } else if (isPowerOfTwoPlusOne(abs)) {
                abs -= 1;
                k = Integer.toBinaryString(abs).length() - 1;
                Register tempReg = getFreeTempReg();
                new IInst(mBasicBlock, IInst.Operation.SLL, tempReg, lReg, new ImmediateNumber(String.valueOf(k)));
                new RInst(mBasicBlock, RInst.Operation.ADDU, lReg, lReg, tempReg);
                setTSRegFree(tempReg);
            } else if (isPowerOfTwoMinusOne(abs)) {
                abs += 1;
                k = Integer.toBinaryString(abs).length() - 1;
                Register tempReg = getFreeTempReg();
                new IInst(mBasicBlock, IInst.Operation.SLL, tempReg, lReg, new ImmediateNumber(String.valueOf(k)));
                new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, tempReg, lReg);
                setTSRegFree(tempReg);
            } else {
                Register temp = getFreeTempReg();
                ImmediateNumber imm = new ImmediateNumber(((ConstVal) rv).getValue());
                new IInst(mBasicBlock, IInst.Operation.LI, temp, null, imm);
                rReg = temp;
                new RInst(mBasicBlock, RInst.Operation.MUL, lReg, lReg, rReg);
                setTSRegFree(rReg);
            }
            //step3:n = n' - n
            new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, tmp, lReg);
            if(x<0){
                new RInst(mBasicBlock, RInst.Operation.SUBU, lReg, Register.R0, lReg);
            }
            setTSRegFree(tmp);
            new MVarSymbol(curTable, inst.getResult().getName(), lReg);
            return;
        } else {
            MVarSymbol lSymbol = (MVarSymbol) curTable.findSymbolInAll(lv.getName());
            lReg = loadSymbolValue(mBasicBlock, lSymbol);
        }
        MVarSymbol rSymbol = (MVarSymbol) curTable.findSymbolInAll(rv.getName());
        rReg = loadSymbolValue(mBasicBlock, rSymbol);
        new RInst(mBasicBlock, RInst.Operation.REM, lReg, lReg, rReg);
        MValue save = saveTempReg(mBasicBlock, lReg);
        new MVarSymbol(curTable, inst.getResult().getName(), save);
        setTSRegFree(rReg);
    }

    private boolean isPowerOfTwo(int n) {
        return (n > 0) && ((n & (n - 1)) == 0);
    }

    private boolean isPowerOfTwoMinusOne(int n) {
        return (n > 0) && ((n & (n + 1)) == 0);
    }

    private boolean isPowerOfTwoPlusOne(int n) {
        return (n > 0) && (((n - 1) & (n - 2)) == 0);
    }

    private void buildGlobalVar(List<GlobalVar> globalVars) {
        for (GlobalVar var : globalVars) {
            String name = var.getName();
            MGlobalVar.InitType type;
            List<String> inits = new ArrayList<>();
            if (var.getValues() != null) {
                type = MGlobalVar.InitType.WORD;
                for (int i = 0; i < var.getValues().size(); i++) {
                    inits.add(var.getValues().get(i).getValue());
                }
            } else {
                int length = 4;
                for (Integer size : var.getType().getLens()) {
                    length *= size;
                }
                type = MGlobalVar.InitType.SPACE;
                inits.add(String.valueOf(length));
            }
            new MVarSymbol(curTable, name, new MemAddr(new Label("zzq_global_" + name.substring(1)), new ImmediateNumber("0")));
            new MGlobalVar(module, type, "zzq_global_" + name.substring(1), inits);
        }
    }

    private void getBlockLiveGlobalRegs(BasicBlock block, MBasicBlock mBasicBlock) {
        if(block.getIn()==null)
            return;
        Set<Value> out = new HashSet<>(block.getIn());
        TreeSet<Register> save = new TreeSet<>();
        for(Value value : curFunction.getGlobalValueRegMap().keySet()){
            if(out.contains(value)){
                save.add(curFunction.getGlobalValueRegMap().get(value));
            }
        }
        blockLiveGlobalRegs.put(mBasicBlock, save);
    }

    private Register loadSymbolValue(MBasicBlock mBasicBlock, MVarSymbol symbol) {
        MValue value = symbol.getValue();
        if (value instanceof Register reg)
            return reg;
        else if (value instanceof MemAddr addr) {
            Register reg = getFreeTempReg();
            new IInst(mBasicBlock, IInst.Operation.LW, addr.getBase(), reg, addr.getOffset());
            return reg;
        }
        return null;
    }

    private MValue saveTempReg(MBasicBlock mBasicBlock, Register source) {
        if (source.isMySaved())
            return source;
        MValue save = getFreeSavedReg();
        //System.out.println("save "+source+" in "+save);
        //printAllSavedReg();
        if (save != null) {
            new IInst(mBasicBlock, IInst.Operation.MOVE, save, source, null);
        } else {
            MemAddr sMem = curFunction.getFrameStack().allocTemp(1, false);
            new IInst(mBasicBlock, IInst.Operation.SW, sMem.getBase(), source, sMem.getOffset());
            save = sMem;
        }
        setTSRegFree(source);
        return save;
    }

    private MemAddr saveTempReg(MBasicBlock mBasicBlock, Register source, Immediate offset) {
        if (source.isMySaved())
            return new MemAddr(source, offset);
        MemAddr save;
        Register reg = getFreeSavedReg();
        if (reg != null) {
            new IInst(mBasicBlock, IInst.Operation.MOVE, reg, source, null);
            save = new MemAddr(reg, offset);
        } else {
            save = curFunction.getFrameStack().allocTemp(1, true);
            new IInst(mBasicBlock, IInst.Operation.ADDIU, source, source, offset);
            new IInst(mBasicBlock, IInst.Operation.SW, save.getBase(), source, save.getOffset());
        }
        setTSRegFree(source);
        return save;
    }

    private MemAddr saveRegsForCall(MBasicBlock mBasicBlock, Register source){
        MemAddr sMem = curFunction.getFrameStack().allocTemp(1, false);
        new IInst(mBasicBlock, IInst.Operation.SW, sMem.getBase(), source, sMem.getOffset());
        return sMem;
    }

    private void saveGlobalVal(Value source){
        FrameStack fs = curFunction.getFrameStack();
        //Register global = getFreeGlobalReg(source);
        Register global = curFunction.getGlobalReg(source);
        if(global==null){
            MemAddr addr = fs.allocTemp(1, false);
            new MVarSymbol(curTable, source.getName(), addr);
        }else {
            new MVarSymbol(curTable, source.getName(), global);
        }
    }

    private Register getFreeSavedReg() {
        Register reg = registerManager.getFreeSavedReg();
        //System.out.println("get free "+reg);
        if (reg == null) {
            System.out.println("no free savedReg");
        } //else System.out.println("alloc free reg " + reg);
        return reg;
    }

    private Register getFreeTempReg() {
        Register reg = registerManager.getFreeTReg();
        if (reg == null) {
            System.out.println("no free reg");
        } //else System.out.println("alloc free reg " + reg);
        return reg;
    }

    private void setTSRegFree(MValue reg) {
        if (reg instanceof Register r) {
            registerManager.setTSRegFree(r);
            //System.out.println("set reg " + reg + " free");
        }
    }

    private void printAllSavedReg() {
        registerManager.printAllSavedRegs();
    }

    private void getGlobalRegMap2(MFunction function, Map<Value, Set<Value>> conflictGraph, Queue<Value> refCounter){
        if(conflictGraph==null || conflictGraph.keySet().isEmpty())
            return;
        int k = registerManager.getGlobalCnt();
        Stack<Value> stack = new Stack<>();
        Map<Value, List<Value>> graph = new HashMap<>();
        for(Map.Entry<Value, Set<Value>> entry: conflictGraph.entrySet()){
            if(!graph.containsKey(entry.getKey())){
                graph.put(entry.getKey(), new ArrayList<>());
            }
            graph.get(entry.getKey()).addAll(entry.getValue());
        }
        while(true){
            boolean goon = false;
            List<Value> values = graph.keySet().stream().toList();
            for(Value value : values){
                int l = graph.get(value).size();
                if(l<k){
                    for(Value v : graph.keySet()){
                        graph.get(v).remove(value);
                    }
                    graph.remove(value);
                    stack.push(value);
                    goon = true;
                }
            }
            if(!goon)
                break;
        }
//        System.out.println(curFunction.getName()+ " graph step 1:");
//        System.out.println(graph);

        List<Value> list = refCounter.stream().toList();
//        System.out.println("all globals:"+list);
        for(int i = list.size()-1;i>=0;i--){
            Value value = list.get(i);
            if(graph.keySet().size()<=1)
                break;
            if(graph.containsKey(value)){
                for(Value v : graph.keySet()){
                    graph.get(v).remove(value);
                }
                graph.remove(value);
                stack.push(value);
            }
        }
        //System.out.println("debug:"+stack);
//        System.out.println(curFunction.getName()+ " graph step 2:");
//        System.out.println(graph);

        //Set<Register> globals = registerManager.getAllGlobalReg();
        Map<Value, Register> valueRegMap = new HashMap<>();
        Value cur = null;
        if(!graph.isEmpty())
            cur = graph.keySet().iterator().next();

        while(true){
            if(cur!=null){
                Set<Register> nearRegs = new HashSet<>();
                for(Value near : graph.get(cur)){
                    Register nearReg = valueRegMap.get(near);
                    if(nearReg!=null)
                        nearRegs.add(nearReg);
                }
                System.out.println("cur "+cur+" nearRegs: "+nearRegs);
                Register curReg = registerManager.getAGlobal(nearRegs);
                if(curReg!=null) {
                    //System.out.println("assign "+curReg+" to "+cur);
                    valueRegMap.put(cur, curReg);
                }else {
                    //System.out.println("don't have reg for "+cur);
                }
            }
            if(stack.isEmpty())
                break;
            Value next = stack.pop();
            graph.put(next, new ArrayList<>());
            for(Value v : conflictGraph.get(next)){
                if(graph.containsKey(v)){
                    graph.get(next).add(v);
                    graph.get(v).add(next);
                }
            }
            cur = next;
        }
        System.out.println(refCounter);
        System.out.println(function.getName()+" Regs: "+valueRegMap);
        function.setGlobalValueRegMap2(valueRegMap);
    }

//    private int saveSomeSavedReg(MFunction function, MBasicBlock mBasicBlock) {
//        Set<Register> regs = funcRegs.get(function.getName());
//        for (Register reg : regs) {
//            FrameStack fs = function.getFrameStack();
//            MemAddr addr = fs.allocReg(1);
//            new IInst(mBasicBlock, IInst.Operation.SW, addr.getBase(), reg, addr.getOffset());
//        }
//        registerManager.saveStatus();
//        return regs.size();
//    }
//
//    private void recoverSomeSavedRegs(MFunction function, MBasicBlock mBasicBlock) {
//        int index = 2;
//        Set<Register> regs = funcRegs.get(function.getName());
//        for (Register reg : regs) {
//            FrameStack fs = function.getFrameStack();
//            MemAddr addr = fs.getRegAddr(index);
//            new IInst(mBasicBlock, IInst.Operation.LW, addr.getBase(), reg, addr.getOffset());
//            index++;
//        }
//        registerManager.recoverStatus();
//    }
//    private Register getFreeGlobalReg(Value value) {
//        //
//        if(curFunction.getGlobalValueRegMap()!=null && curFunction.getGlobalValueRegMap().containsKey(value)){
//            return curFunction.getGlobalValueRegMap().get(value);
//        }else {
//            Set<Value> set = curFunction.getGlobalRegValue();
//            if(set!=null){
//                if (set.contains(value)) {
//                    System.out.println("assign global to " + value);
//                    return registerManager.getFreeGlobalReg();
//                } else
//                    return null;
//            }else
//                return registerManager.getFreeGlobalReg();
//        }
//    }

//    private void saveAllSavedReg(MBasicBlock mBasicBlock) {
//        for (Register reg : registerManager.getAllSavedRegisters()) {
//            FrameStack fs = curFunction.getFrameStack();
//            MemAddr addr = fs.allocReg(1);
//            new IInst(mBasicBlock, IInst.Operation.SW, addr.getBase(), reg, addr.getOffset());
//        }
//        registerManager.saveStatus();
//    }
//
//    private void recoverAllSavedRegs(MBasicBlock mBasicBlock) {
//        int index = 2;
//        for (Register reg : registerManager.getAllSavedRegisters()) {
//            FrameStack fs = curFunction.getFrameStack();
//            MemAddr addr = fs.getRegAddr(index);
//            new IInst(mBasicBlock, IInst.Operation.LW, addr.getBase(), reg, addr.getOffset());
//            index++;
//        }
//        registerManager.recoverStatus();
//    }
//private void setGlobalRegFree(Register reg){
//    registerManager.setGlobalRegFree(reg);
//}
//
//
//

//    private void saveGlobalVal(MemAddr source){
//        FrameStack fs = curFunction.getFrameStack();
//        Register global = getFreeGlobalReg();
//        if(global==null){
//            new MVarSymbol(curTable, source.getName(), fs.allocTemp(1, false));
//        }else {
//            new MVarSymbol(curTable, source.getName(), global);
//        }
//    }


//    private void insert2RegGlobalMap(Register reg, Value value){
//        if(!regGlobalMap.containsKey(reg)){
//            regGlobalMap.put(reg, new HashSet<>());
//        }
//        regGlobalMap.get(reg).add(value);
//    }
}