package midend;

import frontend.Tokens;
import midend.Value.*;
import midend.Value.Instructions.*;

import java.util.ArrayList;
import java.util.List;

public class IRBuildFactory {

    private Function curFunction;
    private BasicBlock curBlock;

    public IRBuildFactory() {
        curFunction = new Function("Global", null, new IRModule());
        curBlock = new BasicBlock(null, curFunction);
    }

    private String nextLabel() {
        return curFunction.nextLabel();
    }

    private String nextReg() {
        return curFunction.nextReg();
    }

    public Value buildGlobalVar(IRModule module, String identName, ValueType type, Value value, boolean isConst) {
        assert value instanceof ConstVal : "BuildGlobalVar: GlobalVar must be initialized by constVal";
        System.out.println("buildGlobalVar: start build " + identName);
        GlobalVar val = new GlobalVar("@" + identName, type, isConst, (ConstVal) value);
        module.addGlobalVar(val);
        return val;
    }

    public Value buildGlobalVar(IRModule module, Tokens.Token ident, ValueType type, List<Value> values, boolean isConst) {
        GlobalVar val = null;
        if (values == null)
            val = new GlobalVar("@" + ident.getValue(), type, null, isConst);
        else {
            List<ConstVal> initVals = new ArrayList<>();
            for (Value value : values) {
                assert value instanceof ConstVal : "BuildGlobalVar: GlobalVar must be initialized by constVal";
                initVals.add((ConstVal) value);
            }
            val = new GlobalVar("@" + ident.getValue(), type, initVals, isConst);
        }
        module.addGlobalVar(val);
        return val;
    }

    public Value buildLocalVar(ValueType type, Value value) {
        //System.out.println("create local , init " + value);
        Instruction allocInst = new AllocaInst(type.getPointer(), nextReg());
        curBlock.addInst(allocInst);
        if (value != null) {
            Instruction storeInst = new StoreInst(value, allocInst.getResult(), allocInst.getResult());
            curBlock.addInst(storeInst);
        }
        return allocInst.getResult();
    }

    public Value buildLocalVar(ValueType type, List<Value> values) {
        //System.out.println("create local , init " + value);
        Instruction allocInst = new AllocaInst(type.getPointer(), nextReg());
        curBlock.addInst(allocInst);
        if (values != null) {
            List<Integer> lens = type.getLens();
            if (type.getDimension() == 1) {
                for (int i = 0; i < lens.get(0); i++) {
                    List<Value> indexs = new ArrayList<>();
                    indexs.add(new ConstVal(new ValueType(ValueType.IRType.INT32, false), "0"));
                    ConstVal index = new ConstVal(new ValueType(ValueType.IRType.INT32, false), String.valueOf(i));
                    indexs.add(index);
                    GetElementPtrInst getElementPtrInst = new GetElementPtrInst(type, allocInst.getResult(), indexs, nextReg());
                    StoreInst storeInst = new StoreInst(values.get(i), getElementPtrInst.getResult(), allocInst.getResult());
                    curBlock.addInst(getElementPtrInst);
                    curBlock.addInst(storeInst);
                }
            } else if (type.getDimension() == 2) {
                for (int i = 0; i < lens.get(0); i++) {
                    for (int j = 0; j < lens.get(1); j++) {
                        List<Value> indexs = new ArrayList<>();
                        indexs.add(new ConstVal(new ValueType(ValueType.IRType.INT32, false), "0"));
                        ConstVal index1 = new ConstVal(new ValueType(ValueType.IRType.INT32, false), String.valueOf(i));
                        ConstVal index2 = new ConstVal(new ValueType(ValueType.IRType.INT32, false), String.valueOf(j));
                        indexs.add(index1);
                        indexs.add(index2);
                        GetElementPtrInst getElementPtrInst = new GetElementPtrInst(type, allocInst.getResult(), indexs, nextReg());
                        StoreInst storeInst = new StoreInst(values.get(i * lens.get(1) + j), getElementPtrInst.getResult(), allocInst.getResult());
                        curBlock.addInst(getElementPtrInst);
                        curBlock.addInst(storeInst);
                    }
                }
            } else if (type.getDimension() == 3) {
                for (int i = 0; i < lens.get(0); i++) {
                    for (int j = 0; j < lens.get(1); j++) {
                        for (int k = 0; k < lens.get(2); k++) {
                            List<Value> indexs = new ArrayList<>();
                            indexs.add(new ConstVal(new ValueType(ValueType.IRType.INT32, false), "0"));
                            ConstVal index1 = new ConstVal(new ValueType(ValueType.IRType.INT32, false), String.valueOf(i));
                            ConstVal index2 = new ConstVal(new ValueType(ValueType.IRType.INT32, false), String.valueOf(j));
                            ConstVal index3 = new ConstVal(new ValueType(ValueType.IRType.INT32, false), String.valueOf(k));
                            indexs.add(index1);
                            indexs.add(index2);
                            indexs.add(index3);
                            GetElementPtrInst getElementPtrInst = new GetElementPtrInst(type, allocInst.getResult(), indexs, nextReg());
                            StoreInst storeInst = new StoreInst(values.get(i * lens.get(2) * lens.get(1) + j * lens.get(1) + k), getElementPtrInst.getResult(), allocInst.getResult());
                            curBlock.addInst(getElementPtrInst);
                            curBlock.addInst(storeInst);
                        }
                    }
                }
            } else {
                System.out.println("buildLocalVar: only support 1D-3D array");
            }
        }
        return allocInst.getResult();
    }


    public Function buildFunction(String name, ValueType retType, IRModule module) {
        name = "@" + name;
        curFunction = new Function(name, retType, module);
        buildBlock();
        return curFunction;
    }

    public Function buildFunction(String name, ValueType retType, IRModule module, List<ValueType> params, List<Value> regs) {
        name = "@" + name;
        curFunction = new Function(name, retType, module);
        for (ValueType pType : params) {
            curFunction.addParam(new Value(nextReg(), pType, true));
        }
        BasicBlock initBlock = buildBlock();
        for (Value p : curFunction.getParams()) {
            regs.add(buildLocalVar(p.getType(), p));
        }
        BasicBlock next = buildBlock();
        initBlock.setEndInst(new BrInst(next));

        return curFunction;
    }

    public BasicBlock buildBlock() {
        return curBlock = new BasicBlock(nextLabel(), curFunction);
    }

    //this function is dangerous!!!!
    public BasicBlock buildBlockWithoutLabel() {
        return curBlock = new BasicBlock(null, curFunction);
    }

    public void assignBlockLabel(BasicBlock block) {
        block.setLabel(nextLabel());
    }

    public Value buildBinaryInst(Value lVar, Value rVar, Tokens.TokenKind op) {

        if (lVar.getType().isType(ValueType.IRType.INT1, 0))
            lVar = buildZextInst(lVar, new ValueType(ValueType.IRType.INT32, false));
        if (rVar.getType().isType(ValueType.IRType.INT1, 0))
            rVar = buildZextInst(rVar, new ValueType(ValueType.IRType.INT32, false));
        if (!lVar.getType().isType(ValueType.IRType.INT32, 0) || !rVar.getType().isType(ValueType.IRType.INT32, 0)) {
            System.out.println("buildBinaryInst: only int32 can do buildBinaryInst");
            return null;
        }
        Instruction instruction = null;
        switch (op) {
            case PLUS -> {
                instruction = new AddInst(lVar, rVar, nextReg());
            }
            case MINU -> {
                instruction = new SubInst(lVar, rVar, nextReg());
            }
            case MULT -> {
                instruction = new MulInst(lVar, rVar, nextReg());
            }
            case MOD -> {
                instruction = new SremInst(lVar, rVar, nextReg());
            }
            case DIV -> {
                instruction = new SdivInst(lVar, rVar, nextReg());
            }
            case GRE, LSS, GEQ, LEQ, EQL, NEQ -> instruction = new IcmpInst(lVar, rVar, nextReg(), op);
        }

        assert instruction != null;
        curBlock.addInst(instruction);
        return instruction.getResult();
    }

    public Value buildUnaryInst(Value value, Tokens.Token op) {
        Instruction instruction = null;
        ConstVal zero = new ConstVal(value.getType(), "0");
        switch (op.getKind()) {
            case PLUS -> {
                return value;
            }
            case MINU -> instruction = new SubInst(zero, value, nextReg());
            case NOT ->
                    instruction = new IcmpInst(value, new ConstVal(new ValueType(ValueType.IRType.INT32, false), "0"), nextReg(), Tokens.TokenKind.EQL);
        }
        assert instruction != null : "buildUnaryInst: Unary OP is illegal";
        curBlock.addInst(instruction);
        return instruction.getResult();
    }

    public Value buildZextInst(Value value, ValueType desType) {
        ZextInst zextInst = new ZextInst(value, desType, nextReg());
        curBlock.addInst(zextInst);
        return zextInst.getResult();
    }

    public void buildStoreInst(Value lValue, Value res, Value real) {
        StoreInst storeInst = new StoreInst(res, lValue, real);
        curBlock.addInst(storeInst);
    }

    public Value buildCallInst(Function function, List<Value> params) {
        CallInst callInst = null;
        if (function.getType().isType(ValueType.IRType.VOID, 0))
            callInst = new CallInst(function, params);
        else
            callInst = new CallInst(function, params, nextReg());
        if (function.isPutch()) {
            curBlock.addInst(callInst);
        } else {
            BasicBlock pre = curBlock;
            BasicBlock cur = buildBlock();
            BrInst end = pre.getEndInst();
            pre.setEndInst(new BrInst(cur));
            cur.addInst(callInst);
            BasicBlock next = buildBlock();
            cur.setEndInst(new BrInst(next));
            if (end != null)
                next.setEndInst(end);
        }
        return callInst.getResult();
    }

//    public void buildPutString(Function cPrinter, Function intPrinter, List<Value> values, String fmt) {
//        char[] chars = fmt.toCharArray();
//        int pos = 0;
//        for (int i = 1; i < chars.length - 1; i++) {
//            List<Value> param = new ArrayList<>();
//            if (chars[i] == '%') {
//                param.add(values.get(pos++));
//                i++;
//                switch (chars[i]) {
//                    case 'd':
//                        buildCallInst(intPrinter, param);
//                }
//            } else if (chars[i] == '\\') {
//                switch (chars[i + 1]) {
//                    case 'n':
//                        chars[i + 1] = 10;
//                }
//            } else {
//                param.add(new ConstVal(new ValueType(ValueType.IRType.INT32, false), String.valueOf((int) chars[i])));
//                buildCallInst(cPrinter, param);
//            }
//        }
//    }

    public void setBlockNext(BasicBlock block) {
        BrInst brInst = new BrInst(block);
        curBlock.setEndInst(brInst);
    }

    public void setBlockNext(Value value, BasicBlock trueBlock, BasicBlock falseBlock) {
        BrInst brInst;
        if (value instanceof ConstVal c) {
            if (c.getValue().equals("true"))
                brInst = new BrInst(trueBlock);
            else
                brInst = new BrInst(falseBlock);
        } else
            brInst = new BrInst(value, trueBlock, falseBlock);
        curBlock.setEndInst(brInst);
    }

    public void buildBrInst(BasicBlock block) {
        BrInst brInst = new BrInst(block);
        curBlock.addInst(brInst);
        curBlock.setBlockEnd();
    }

    public void buildRetInst(Value value) {
        RetInst retInst;
        if (value == null) {
            retInst = new RetInst();
        } else {
            retInst = new RetInst(value);
        }
        curBlock.addInst(retInst);
        curBlock.setBlockEnd();
    }

    public Value buildGetElementPtrInst(ValueType basicType, Value source, List<Value> indexs) {
        Instruction inst = new GetElementPtrInst(basicType, source, indexs, nextReg());
        curBlock.addInst(inst);
        return inst.getResult();
    }

    public Value buildLoadInst(Value value, Value real) {
        assert value.getType().isPointer();
        LoadInst loadInst = new LoadInst(value, nextReg(), real);
        curBlock.addInst(loadInst);
        return loadInst.getResult();
    }

    public BasicBlock getCurBlock() {
        return curBlock;
    }

    public void setCurBlock(BasicBlock block) {
        curBlock = block;
    }
}
