package midend;

import midend.IRTable.IRFuncSymbol;
import midend.IRTable.IRSymbolTable;
import midend.IRTable.IRVarSymbol;
import midend.Value.*;
import frontend.Tokens;
import frontend.Unit;
import frontend.tree.Def;
import frontend.tree.Number;
import frontend.tree.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Visitor {
    private final CompUnit compUnit;
    private final IRBuildFactory builder;

    private final BufferedWriter writer;
    private final IRModule module;

    private IRSymbolTable curSymbolTable;

    private boolean GlobalDef = true;

    public Visitor(CompUnit compUnit, IRBuildFactory irBuildFactory, BufferedWriter outputWriter) {
        this.compUnit = compUnit;
        this.builder = irBuildFactory;
        this.module = new IRModule();
        this.writer = outputWriter;
        curSymbolTable = new IRSymbolTable(null);
        //curSymbolTable.log(0);
    }

    public void declareLib() throws IOException {
        ValueType type = new ValueType(ValueType.IRType.INT32, false);
        Function func = new Function("@getint", type, null);
        curSymbolTable.AddSymbol(new IRFuncSymbol("getint", type, func));

        type = new ValueType(ValueType.IRType.VOID, false);
        func = new Function("@putch", type, null);
        curSymbolTable.AddSymbol(new IRFuncSymbol("putch", type, func));

        type = new ValueType(ValueType.IRType.VOID, false);
        func = new Function("@putint", type, null);
        curSymbolTable.AddSymbol(new IRFuncSymbol("putint", type ,func));
    }

    public IRModule generateLLVM() throws IOException {
        declareLib();
        visit(compUnit);
        module.printModule(writer);
        return module;
    }

    public void visit(CompUnit root) {
        // Decl
        // FuncDef
        System.out.println("---------Global Declare----------");
        for (Unit unit : root.derivations) {
            if (unit instanceof MainFuncDef) {
                GlobalDef = false;
                System.out.println("---------main----------");
                visit((MainFuncDef) unit);
            } else if (unit instanceof ConstDecl) {
                visit((ConstDecl) unit);
            } else if (unit instanceof VarDecl) {
                visit((VarDecl) unit);
            } else if (unit instanceof FuncDef) {
                visit((FuncDef) unit);
            }
        }
    }

    public void visit(FuncDef funcDef) {
        ValueType type = null;
        String name = null;
        List<FuncFParam> params = new ArrayList<>();
        List<Value> pRegs = new ArrayList<>();
        for (Unit unit : funcDef.derivations) {
            if (unit instanceof FuncType) {
                type = ValueType.toValueType((Tokens.Token) ((FuncType) unit).derivations.get(0));
            } else if (unit instanceof Tokens.Token && ((Tokens.Token) unit).getKind() == Tokens.TokenKind.IDENT) {
                name = ((Tokens.Token) unit).getValue();
            } else if (unit instanceof FuncFParams) {
                for (Unit p : ((FuncFParams) unit).derivations) {
                    if (p instanceof FuncFParam) {
                        params.add((FuncFParam) p);
                    }
                }
            }
        }

        System.out.println("-----Func generation start: " + name + "-----");

        assert type != null;
        List<ValueType> types = new ArrayList<>();

        for (FuncFParam param : params) {
            ValueType bType = ValueType.toValueType((Tokens.Token) param.derivations.get(0));
            if (param.derivations.size() == 2) {
                types.add(bType);
            } else {
                int d = 0;
                boolean ignoreFirst = ((Tokens.Token) param.derivations.get(2)).getKind() != Tokens.TokenKind.LBRACK
                        || ((Tokens.Token) param.derivations.get(3)).getKind() != Tokens.TokenKind.RBRACK;
                boolean first = true;
                List<Integer> lens = new ArrayList<>();
                for (Unit unit : param.derivations) {
                    if (unit instanceof ConstExp exp) {
                        if (!first || !ignoreFirst) {
                            ConstVal value = visit(exp);
                            d++;
                            lens.add(Integer.parseInt(value.getValue()));
                            first = false;
                        }
                    }
                }
                ValueType aType = bType.getArray(d, lens);
                aType = aType.getArrayPointer();
                types.add(aType);
            }
        }
        Function func = builder.buildFunction(name, type, module, types, pRegs);
        curSymbolTable.AddSymbol(new IRFuncSymbol(name, type, func));
        curSymbolTable = new IRSymbolTable(curSymbolTable);
        assert params.size() == pRegs.size();
        for (int i = 0; i < params.size(); i++) {
            String pName = params.get(i).getIdent().getValue();
            Value pValue = pRegs.get(i);
            curSymbolTable.AddSymbol(new IRVarSymbol(pName, pValue.getType(), pValue, false));
        }
        GlobalDef = false;
        visit(funcDef.getBlock(), false, null, null);
        GlobalDef = true;
        //System.out.println("-----Func done: " + name+"-----");
    }

    private void visit(VarDecl decl) {
        ValueType type = ValueType.toValueType((Tokens.Token) decl.derivations.get(0));
        for (Unit varDef : decl.derivations) {
            if (varDef instanceof VarDef def) {
                visit(def, type);
            }
        }
    }

    private Value visit(InitVal initVal) {
        return visit((Exp) initVal.derivations.get(0));
    }

    private void visit(ConstDecl decl) {
        ValueType type = ValueType.toValueType((Tokens.Token) decl.derivations.get(1));
        for (Unit constDef : decl.derivations) {
            if (constDef instanceof ConstDef def) {
                visit(def, type);
            }
        }
    }

    private ConstVal visit(ConstInitVal constInitVal) {
        ConstExp constExp = (ConstExp) constInitVal.derivations.get(0);
        return (ConstVal) visit((AddExp) constExp.derivations.get(0));
    }

    private void visit(Def def, ValueType type) {
        boolean isConst = def instanceof ConstDef;
        List<ConstExp> dimensions = def.getDimensions();
        int dimension = dimensions.size();
        Tokens.Token ident = (Tokens.Token) def.derivations.get(0);
        Value p_val;
        //普通变量
        if (dimensions.isEmpty()) {
            Value value = null;
            if (def.derivations.size() > 1) {
                if (isConst) value = visit((ConstInitVal) (def).derivations.get(2));
                else value = visit((InitVal) def.derivations.get(2));
            }
            if (GlobalDef && value == null) {
                value = defaultInit(type);
            }
            if(isConst){
                p_val = value;
            }else {
                if (GlobalDef) {
                    assert value instanceof ConstVal : "Global Def must be const";
                    p_val = builder.buildGlobalVar(module, ident.getValue(), type, value, isConst);
                    //System.out.println("GlobalVar: " + ident.getValue());
                } else {
                    p_val = builder.buildLocalVar(type, value);
                }
            }
            IRVarSymbol symbol = new IRVarSymbol(ident.getValue(), p_val.getType(), p_val, isConst);
            curSymbolTable.AddSymbol(symbol);
        }
        //数组
        else {
            List<Integer> lens = new ArrayList<>();
            for (ConstExp exp : dimensions) {
                ConstVal value = visit(exp);
                lens.add(Integer.parseInt(value.getValue()));
            }
            type = type.getArray(dimension, lens);
            BasicInitVal initVal = def.getInitVal();
            List<Value> initVals = null;
            if (initVal != null) {
                initVals = new ArrayList<>();
                getAllValuesInInitVal(initVal, initVals);
            }
            if (GlobalDef) {
                p_val = builder.buildGlobalVar(module, ident, type, initVals, isConst);
            } else {
                p_val = builder.buildLocalVar(type, initVals);
            }
            IRVarSymbol symbol = new IRVarSymbol(ident.getValue(), p_val.getType(), p_val, isConst);
            curSymbolTable.AddSymbol(symbol);
            if(isConst){
                List<ConstVal> initConsts = new ArrayList<>();
                assert initVals != null;
                for(Value v : initVals){
                    initConsts.add((ConstVal) v);
                }
                symbol.consts = initConsts;
            }
        }
    }

    private void getAllValuesInInitVal(BasicInitVal initVal, List<Value> initVals) {
        if (initVal.derivations.get(0) instanceof Exp exp) {
            Value value = visit(exp);
            initVals.add(value);
        } else if (initVal.derivations.get(0) instanceof ConstExp exp) {
            Value value = visit(exp);
            initVals.add(value);
        }
        for (Unit unit : initVal.derivations) {
            if (unit instanceof BasicInitVal) {
                getAllValuesInInitVal((BasicInitVal) unit, initVals);
            }
        }
    }

    private ConstVal visit(ConstExp constExp) {
        return (ConstVal) visit((AddExp) constExp.derivations.get(0));
    }

    private void visit(MainFuncDef root) {
        builder.buildFunction("main", new ValueType(ValueType.IRType.INT32, false), module);
        for (Unit unit : root.derivations) {
            if (unit instanceof Block) {
                visit((Block) unit, true, null, null);
            }
        }
    }

    private void visit(Block root, boolean needTable, BasicBlock out, BasicBlock fs2) {
        if (needTable)
            curSymbolTable = new IRSymbolTable(curSymbolTable);
        for (Unit unit : root.derivations) {
            if (unit instanceof Stmt) {
                visit((Stmt) unit, out, fs2);
            } else if (unit instanceof ConstDecl) {
                visit((ConstDecl) unit);
            } else if (unit instanceof VarDecl) {
                visit((VarDecl) unit);
            }
        }
        curSymbolTable = curSymbolTable.getParent();
    }

    private void visit(Stmt root, BasicBlock out, BasicBlock fs2) {
        // others
        switch (root.getType()) {
            case RETURN -> {
                Exp exp = null;
                for (Unit unit : root.derivations) {
                    if (unit instanceof Exp) {
                        exp = (Exp) unit;
                    }
                }
                if (exp == null)
                    builder.buildRetInst(null);
                else
                    builder.buildRetInst(visit(exp));
                //builder.buildBlock();
            }
            case LVAL -> {
                LVal lVal = (LVal) root.derivations.get(0);
                Exp exp = (Exp) root.derivations.get(2);
                assignLVal(lVal, exp);
            }
            case BLOCK -> {
                visit((Block) root.derivations.get(0), true, out, fs2);
            }
            case GETINT -> {
                IRFuncSymbol funcSymbol = (IRFuncSymbol) curSymbolTable.findSymbolInAll("getint");
                Function function = funcSymbol.getFunction();
                Value res = builder.buildCallInst(function, null);
                assignLVal((LVal) root.derivations.get(0), res);
            }
            case PRINTF -> {
                List<Value> exps = new ArrayList<>();
                String fmtString = null;
                for (Unit unit : root.derivations) {
                    if (unit instanceof Exp exp)
                        exps.add(visit(exp));
                    if (unit instanceof Tokens.Token && ((Tokens.Token) unit).getKind() == Tokens.TokenKind.FMTSTRING)
                        fmtString = ((Tokens.Token) unit).getValue();
                }
                assert fmtString != null;
                IRFuncSymbol putchFunc = (IRFuncSymbol) curSymbolTable.findSymbolInAll("putch");
                Function cPrinter = putchFunc.getFunction();
                IRFuncSymbol putintFunc = (IRFuncSymbol) curSymbolTable.findSymbolInAll("putint");
                Function intPrinter = putintFunc.getFunction();

                char[] chars = fmtString.toCharArray();
                int pos = 0;
                for(int i = 1;i<chars.length-1;i++){
                    List<Value> param = new ArrayList<>();
                    if(chars[i] == '%'){
                        param.add(exps.get(pos++));
                        i++;
                        switch (chars[i]){
                            case 'd': builder.buildCallInst(intPrinter, param);
                        }
                    }else if(chars[i] == '\\') {
                        switch (chars[i+1]){
                            case 'n': chars[i+1] = 10;
                        }
                    } else{
                        param.add(new ConstVal(new ValueType(ValueType.IRType.INT32, false), String.valueOf((int) chars[i])));
                        builder.buildCallInst(cPrinter, param);
                    }
                }
            }
            case IF -> {
                Cond cond = (Cond) root.derivations.get(2);
                Stmt stmt1 = (Stmt) root.derivations.get(4);
                Stmt stmt2 = null;
                if (root.derivations.size() > 5) stmt2 = (Stmt) root.derivations.get(6);

                BasicBlock curBlock = builder.getCurBlock();

                BasicBlock s1Block = builder.buildBlockWithoutLabel();
                BasicBlock s2Block = null;
                if (stmt2 != null) {
                    s2Block = builder.buildBlockWithoutLabel();
                }
                BasicBlock s3Block = builder.buildBlockWithoutLabel();
                builder.setCurBlock(curBlock);
                visit(cond, s1Block, s2Block == null ? s3Block : s2Block);
                builder.assignBlockLabel(s1Block);
                builder.setCurBlock(s1Block);
                visit(stmt1, out, fs2);
                builder.setBlockNext(s3Block);
                if (s2Block != null) {
                    builder.assignBlockLabel(s2Block);
                    builder.setCurBlock(s2Block);
                    visit(stmt2, out, fs2);
                    builder.setBlockNext(s3Block);
                }
                builder.assignBlockLabel(s3Block);
                builder.setCurBlock(s3Block);
            }
            case EXP -> {
                if (root.derivations.get(0) instanceof Exp exp) {
                    visit(exp);
                }
                //nothing to do...
            }
            case FOR -> {
                ForStmt forStmt1 = null, forStmt2 = null;
                BasicBlock fs1Block = null, fs2Block = null;
                Stmt stmt = null;
                BasicBlock sBlock = null;
                Cond cond = null;
                BasicBlock curBlock = builder.getCurBlock();
                BasicBlock cBlock = null;
                BasicBlock nextBlock = builder.buildBlockWithoutLabel();
                boolean isStmt1 = true;
                for (Unit unit : root.derivations) {
                    if (unit instanceof ForStmt) {
                        if (isStmt1) {
                            forStmt1 = (ForStmt) unit;
                            fs1Block = builder.buildBlockWithoutLabel();
                            fs1Block.setInLoop();
                        } else {
                            forStmt2 = (ForStmt) unit;
                            fs2Block = builder.buildBlockWithoutLabel();
                            fs2Block.setInLoop();
                        }
                    }
                    if (unit instanceof Cond) {
                        cond = (Cond) unit;
                        cBlock = builder.buildBlockWithoutLabel();
                        cBlock.setInLoop();
                    }
                    if (unit instanceof Stmt) {
                        stmt = (Stmt) unit;
                        sBlock = builder.buildBlockWithoutLabel();
                        sBlock.setInLoop();
                    }
                    if (unit instanceof Tokens.Token t && t.getKind() == Tokens.TokenKind.SEMICN)
                        isStmt1 = false;
                }
                builder.setCurBlock(curBlock);
                if (forStmt1 != null) builder.setBlockNext(fs1Block);
                else {
                    if (cond != null) {
                        builder.setBlockNext(cBlock);
                    } else builder.setBlockNext(sBlock);
                }

                if (forStmt1 != null) {
                    builder.setCurBlock(fs1Block);
                    builder.assignBlockLabel(fs1Block);
                    visit(forStmt1);
                    if (cond == null) {
                        builder.setBlockNext(sBlock);
                    } else {
                        builder.setBlockNext(cBlock);
                    }
                }
                if (cond != null) {
                    builder.setCurBlock(cBlock);
                    builder.assignBlockLabel(cBlock);
                    visit(cond, sBlock, nextBlock);
                }

                assert stmt != null;
                builder.setCurBlock(sBlock);
                builder.assignBlockLabel(sBlock);
                if (forStmt2 != null) visit(stmt, nextBlock, fs2Block);
                else if (cond != null) visit(stmt, nextBlock, cBlock);
                else visit(stmt, nextBlock, sBlock);

                if (forStmt2 != null) {
                    builder.setBlockNext(fs2Block);
                } else {
                    if (cond != null) builder.setBlockNext(cBlock);
                    else builder.setBlockNext(sBlock);
                }

                if (forStmt2 != null) {
                    builder.setCurBlock(fs2Block);
                    builder.assignBlockLabel(fs2Block);
                    visit(forStmt2);
                    if (cond != null) {
                        builder.setBlockNext(cBlock);
                    } else builder.setBlockNext(sBlock);
                }

                builder.setCurBlock(nextBlock);
                builder.assignBlockLabel(nextBlock);
            }
            case WHILE ->{
                //此处没有考虑stmt和cond可以为空的情况
                Stmt stmt = null;
                Cond cond = null;
                for (Unit unit : root.derivations) {
                    if(unit instanceof Cond c)
                        cond = c;
                    if(unit instanceof Stmt s)
                        stmt = s;
                }
                BasicBlock curBlock = builder.getCurBlock();
                BasicBlock cBlock = builder.buildBlockWithoutLabel();
                cBlock.setInLoop();
                BasicBlock sBlock = builder.buildBlockWithoutLabel();
                sBlock.setInLoop();
                BasicBlock nextBlock = builder.buildBlockWithoutLabel();
                builder.setCurBlock(curBlock);
                assert cond != null;
                builder.setBlockNext(cBlock);
                builder.assignBlockLabel(cBlock);
                builder.setCurBlock(cBlock);
                visit(cond, sBlock, nextBlock);
                builder.assignBlockLabel(sBlock);
                builder.setCurBlock(sBlock);
                visit(stmt, nextBlock, cBlock);
                builder.setBlockNext(cBlock);
                builder.assignBlockLabel(nextBlock);
                builder.setCurBlock(nextBlock);
            }
            case DO_WHILE -> {
                //此处没有考虑stmt和cond可以为空的情况
                Stmt stmt = null;
                Cond cond = null;
                for (Unit unit : root.derivations) {
                    if(unit instanceof Cond c)
                        cond = c;
                    if(unit instanceof Stmt s)
                        stmt = s;
                }
                BasicBlock curBlock = builder.getCurBlock();
                BasicBlock cBlock = builder.buildBlockWithoutLabel();
                cBlock.setInLoop();
                BasicBlock sBlock = builder.buildBlockWithoutLabel();
                sBlock.setInLoop();
                BasicBlock nextBlock = builder.buildBlockWithoutLabel();
                builder.setCurBlock(curBlock);
                builder.setBlockNext(sBlock);

                builder.assignBlockLabel(sBlock);
                builder.setCurBlock(sBlock);
                visit(stmt, nextBlock, cBlock);
                builder.setBlockNext(cBlock);

                builder.assignBlockLabel(cBlock);
                builder.setCurBlock(cBlock);
                visit(cond, sBlock, nextBlock);

                builder.assignBlockLabel(nextBlock);
                builder.setCurBlock(nextBlock);
            }
            case BREAK -> {
                builder.buildBrInst(out);
                //builder.buildBlock();
            }
            case CONTINUE -> {
                builder.buildBrInst(fs2);
                //builder.buildBlock();
            }

        }
    }

    //此函数未考虑指针变量也可以赋值的情况，故可能产生未知错误
    private void assignLVal(LVal lVal, Exp exp) {
        Tokens.Token lVal_ident = (Tokens.Token) lVal.derivations.get(0);
        IRVarSymbol symbol = (IRVarSymbol) curSymbolTable.findSymbolInAll(lVal_ident.getValue());
        if (symbol.isConst()) {
            System.out.println("assignLVal: you can't assign a const var!");
            return;
        }
        Value rVal = visit(exp);

        Value value = symbol.getValue();
        Value origin = value;
        List<Value> exps = new ArrayList<>();
        for (Unit unit : lVal.derivations) {
            if (unit instanceof Exp e) {
                Value expVal = visit(e);
                exps.add(expVal);
            }
        }

        ConstVal zero = new ConstVal(new ValueType(ValueType.IRType.INT32, false), "0");
        ValueType type = value.getType().dePointer();
        if (type.isPointer() && !exps.isEmpty()) {
            //只会是数组指针
            value = builder.buildLoadInst(value, origin);
            List<Value> d1 = new ArrayList<>();
            d1.add(exps.get(0));
            value = builder.buildGetElementPtrInst(value.getType().dePointer(), value, d1);
            exps.set(0, zero);
            if (exps.size() > 1)
                value = builder.buildGetElementPtrInst(value.getType().dePointer(), value, exps);
        } else if (type.isArray() && !exps.isEmpty()) {
            exps.add(0, zero);
            value = builder.buildGetElementPtrInst(type, value, exps);
        }
        builder.buildStoreInst(value, rVal, origin);
    }

    private void assignLVal(LVal lVal, Value rVal) {
        Tokens.Token lVal_ident = (Tokens.Token) lVal.derivations.get(0);
        IRVarSymbol symbol = (IRVarSymbol) curSymbolTable.findSymbolInAll(lVal_ident.getValue());
        if (symbol.isConst()) {
            System.out.println("assignLVal: you can't assign a const var!");
            return;
        }
        Value value = symbol.getValue();
        Value origin = value;
        List<Value> exps = new ArrayList<>();
        for (Unit unit : lVal.derivations) {
            if (unit instanceof Exp exp) {
                Value expVal = visit(exp);
                exps.add(expVal);
            }
        }
        ConstVal zero = new ConstVal(new ValueType(ValueType.IRType.INT32, false), "0");
        ValueType type = value.getType().dePointer();
        if (type.isPointer() && !exps.isEmpty()) {
            //只会是数组指针
            value = builder.buildLoadInst(value, origin);
            List<Value> d1 = new ArrayList<>();
            d1.add(exps.get(0));
            value = builder.buildGetElementPtrInst(value.getType().dePointer(), value, d1);
            exps.set(0, zero);
            if (exps.size() > 1)
                value = builder.buildGetElementPtrInst(value.getType().dePointer(), value, exps);
        } else if (type.isArray() && !exps.isEmpty()) {
            exps.add(0, zero);
            value = builder.buildGetElementPtrInst(type, value, exps);
        }
        builder.buildStoreInst(value, rVal, origin);
    }

    //会得到LVal的值
    private Value visit(LVal lVal) {
        Tokens.Token lVal_ident = (Tokens.Token) lVal.derivations.get(0);
        List<Value> exps = new ArrayList<>();
        for (Unit unit : lVal.derivations) {
            if (unit instanceof Exp exp) {
                Value expVal = visit(exp);
                exps.add(expVal);
            }
        }
        IRVarSymbol varSymbol = (IRVarSymbol) curSymbolTable.findSymbolInAll(lVal_ident.getValue());
        if(varSymbol.isConst() && !varSymbol.getType().isArray())
            return varSymbol.getValue();
        if (GlobalDef) {
            List<ConstVal> vals = varSymbol.getValue().getValues();
            if (exps.isEmpty())
                return new ConstVal(varSymbol.getValue().getType().dePointer(), vals.get(0).getValue());
            else {
                if (vals.isEmpty()) {
                    return new ConstVal(varSymbol.getValue().getType().getBtype(), "0");
                } else {
                    List<Integer> lens = varSymbol.getValue().getType().getLens();
                    int index = 0;
                    int stride = 1;
                    for (int i = 0; i < lens.size(); i++) {
                        index = Integer.parseInt(((ConstVal) exps.get(i)).getValue()) * stride;
                        stride *= lens.get(i);
                    }
                    return new ConstVal(varSymbol.getValue().getType().getBtype(), vals.get(index).getValue());
                }

            }
        }
        else {
            Value value = varSymbol.getValue();
            Value origin = value;
            ConstVal zero = new ConstVal(new ValueType(ValueType.IRType.INT32, false), "0");
            ValueType type = value.getType().dePointer();
            //System.out.println("visit LVal: "+ type +" " + lVal_ident.getValue() + exps);
            if (type.isPointer()) {
                //只会是数组指针
                //System.out.println("test");
                value = builder.buildLoadInst(value, origin);
                if (!exps.isEmpty()) {
                    List<Value> d1 = new ArrayList<>();
                    d1.add(exps.get(0));
                    value = builder.buildGetElementPtrInst(value.getType().dePointer(), value, d1);
                    exps.set(0, zero);
                    boolean isGetElement = exps.size() == type.getDimension() + 1;
                    if (!isGetElement)
                        exps.add(zero);
                    if (exps.size() > 1)
                        value = builder.buildGetElementPtrInst(value.getType().dePointer(), value, exps);
                    if (isGetElement)
                        value = builder.buildLoadInst(value, origin);
                }
            }
            else if (type.isArray()) {
                //普通数组
                boolean isGetElement = exps.size() == type.getDimension();
                boolean isAllConst = true;
                int pos = 0;
                int i = 0;
                for(Value v : exps){
                    if(v instanceof ConstVal c){
                        pos = pos * type.getLens().get(i) + Integer.parseInt(c.getValue());
                    }else{
                        isAllConst = false;
                        break;
                    }
                    i++;
                }
                if(isAllConst && varSymbol.isConst() && isGetElement){
                    value = varSymbol.consts.get(pos);
                    return value;
                }
                if (!isGetElement)
                    exps.add(zero);
                exps.add(0, zero);
                value = builder.buildGetElementPtrInst(type, value, exps);
                if (isGetElement)
                    value = builder.buildLoadInst(value, origin);
            }
            else {
                value = builder.buildLoadInst(value, origin);
            }
            return value;
        }
    }
    private void visit(ForStmt forStmt) {
        LVal lVal = (LVal) forStmt.derivations.get(0);
        Exp exp = (Exp) forStmt.derivations.get(2);
        assignLVal(lVal, exp);
    }

    private void visit(Cond cond, BasicBlock trueBlock, BasicBlock falseBlock) {
        LOrExp lOrExp = (LOrExp) cond.derivations.get(0);
        visit(lOrExp, trueBlock, falseBlock);
    }

    private void visit(LOrExp lOrExp, BasicBlock trueBlock, BasicBlock falseBlock) {
        LOrExp son = null;
        LAndExp lAndExp = null;
        if (lOrExp.derivations.size() == 1) lAndExp = (LAndExp) lOrExp.derivations.get(0);
        else {
            son = (LOrExp) lOrExp.derivations.get(0);
            lAndExp = (LAndExp) lOrExp.derivations.get(2);
        }
        if (son != null) {
            BasicBlock curBlock = builder.getCurBlock();
            BasicBlock expBlock = builder.buildBlockWithoutLabel();
            builder.setCurBlock(curBlock);
            visit(son, trueBlock, expBlock);
            builder.setCurBlock(expBlock);
            builder.assignBlockLabel(expBlock);
        }
        visit(lAndExp, trueBlock, falseBlock);
    }

    private void visit(LAndExp lAndExp, BasicBlock trueBlock, BasicBlock falseBlock) {
        LAndExp son = null;
        EqExp eqExp = null;
        if (lAndExp.derivations.size() == 1) eqExp = (EqExp) lAndExp.derivations.get(0);
        else {
            son = (LAndExp) lAndExp.derivations.get(0);
            eqExp = (EqExp) lAndExp.derivations.get(2);
        }
        if (son != null) {
            BasicBlock curBlock = builder.getCurBlock();
            BasicBlock expBlock = builder.buildBlockWithoutLabel();
            builder.setCurBlock(curBlock);
            visit(son, expBlock, falseBlock);
            builder.setCurBlock(expBlock);
            builder.assignBlockLabel(expBlock);
        }
        Value res = visit(eqExp);
        res = TypeToInt1(res);
        builder.setBlockNext(res, trueBlock, falseBlock);
    }

    //生成计算该exp的所有指令
    //返回对应的寄存器Value
    private Value visit(Exp exp) {
        //System.out.println(exp);
        AddExp addExp = (AddExp) exp.derivations.get(0);
        return visit(addExp);
    }

    private Value visit(RelExp exp) {
        if (exp.derivations.get(0) instanceof AddExp) {
            return visit((AddExp) exp.derivations.get(0));
        }
        Value l = visit((RelExp) exp.derivations.get(0));
        Tokens.Token op = (Tokens.Token) (exp.derivations.get(1));
        Value r = visit((AddExp) exp.derivations.get(2));
        if (l instanceof ConstVal && r instanceof ConstVal) {
            return binaryCalConst(l, r, op);
        } else {
            return builder.buildBinaryInst(l, r, op.getKind());
        }
    }

    private Value visit(EqExp exp) {
        if (exp.derivations.size() == 1) {
            return visit((RelExp) exp.derivations.get(0));
        }
        Value l = visit((EqExp) exp.derivations.get(0));
        Tokens.Token op = (Tokens.Token) (exp.derivations.get(1));
        Value r = visit((RelExp) exp.derivations.get(2));
        if (l instanceof ConstVal && r instanceof ConstVal) {
            return binaryCalConst(l, r, op);
        } else {
            return builder.buildBinaryInst(l, r, op.getKind());
        }
    }

    private Value visit(AddExp addExp) {
        if (addExp.derivations.get(0) instanceof MulExp) {
            return visit((MulExp) addExp.derivations.get(0));
        }
        Value l = visit((AddExp) addExp.derivations.get(0));
        Tokens.Token op = (Tokens.Token) (addExp.derivations.get(1));
        Value r = visit((MulExp) addExp.derivations.get(2));
        if (l instanceof ConstVal && r instanceof ConstVal) {
            return binaryCalConst(l, r, op);
        } else
            return builder.buildBinaryInst(l, r, op.getKind());
    }

    private Value visit(MulExp mulExp) {
        if (mulExp.derivations.get(0) instanceof UnaryExp) {
            return visit((UnaryExp) mulExp.derivations.get(0));
        }

        Value l = visit((MulExp) mulExp.derivations.get(0));
        Tokens.Token op = (Tokens.Token) (mulExp.derivations.get(1));
        Value r = visit((UnaryExp) mulExp.derivations.get(2));
        if (l instanceof ConstVal && r instanceof ConstVal) {
            return binaryCalConst(l, r, op);
        } else
            return builder.buildBinaryInst(l, r, op.getKind());
    }

    private Value visit(UnaryExp exp) {
        Unit unit = exp.derivations.get(0);
        Value value = null;
        if (unit instanceof PrimaryExp) {
            value = visit((PrimaryExp) unit);
        } else if (unit instanceof UnaryOp) {
            Tokens.Token op = (Tokens.Token) ((UnaryOp) unit).derivations.get(0);
            value = visit((UnaryExp) exp.derivations.get(1));
            if (value instanceof ConstVal) value = unaryCalConst(value, op);
            else value = builder.buildUnaryInst(value, op);
        } else {
            //函数调用
            String funcName = ((Tokens.Token) exp.derivations.get(0)).getValue();
            IRFuncSymbol funcSymbol = (IRFuncSymbol) curSymbolTable.findSymbolInAll(funcName);
            Function function = funcSymbol.getFunction();
            List<Value> values = new ArrayList<>();
            FuncRParams funcRParams = null;
            if (exp.derivations.get(2) instanceof FuncRParams) {
                funcRParams = (FuncRParams) exp.derivations.get(2);
            }
            if (funcRParams != null) {
                for (Unit u : funcRParams.derivations) {
                    if (u instanceof Exp) {
                        values.add(visit((Exp) u));
                    }
                }
            }
            value = builder.buildCallInst(function, values);
        }
        return value;
    }

    private Value visit(PrimaryExp exp) {
        Unit unit = exp.derivations.get(0);
        Value value = null;
        if (unit instanceof LVal) {
            value = visit((LVal) unit);
        } else if (unit instanceof Number) {
            value = visit((Number) unit);
        } else {
            Exp son = (Exp) exp.derivations.get(1);
            value = visit(son);
        }

        return value;
    }

    private ConstVal visit(Number number) {
        Tokens.Token token = (Tokens.Token) number.derivations.get(0);
        ConstVal value = null;
        switch (token.getKind()) {
            case INTCONST -> value = new ConstVal(new ValueType(ValueType.IRType.INT32, false), token.getValue());
        }
        //System.out.println(value);
        return value;
    }

    private ConstVal binaryCalConst(Value lVar, Value rVar, Tokens.Token op) {
        assert lVar instanceof ConstVal && rVar instanceof ConstVal : "binaryCalConst: lVar or rVar is not const";
        if (lVar.getType().isPointer() || lVar.getType().isArray() || rVar.getType().isPointer() || rVar.getType().isArray()) {
            System.out.println("binaryCalConst: don't support pointer or array!");
            return null;
        }
        lVar = int1Totype(lVar);
        rVar = int1Totype(rVar);
        System.out.println("lvar= "+((ConstVal) lVar).getValue() + " rvar= "+((ConstVal) rVar).getValue());
        if (!lVar.getType().isType(ValueType.IRType.INT32, 0) || !lVar.getType().isType(ValueType.IRType.INT32, 0)) {
            System.out.println("binaryCalConst: only int32 can do binaryCalConst");
            return null;
        }
        ValueType resType = lVar.getType();
        String ans;
        int l = Integer.parseInt(((ConstVal) lVar).getValue());
        int r = Integer.parseInt(((ConstVal) rVar).getValue());
        int res = 0;
        switch (op.getKind()) {
            case PLUS -> res = l + r;
            case MINU -> res = l - r;
            case MULT -> res = l * r;
            case DIV -> res = l / r;
            case MOD -> res = l % r;
            case GRE -> {
                ans = l > r ?"true":"false";
                resType = new ValueType(ValueType.IRType.INT1, false);
                return new ConstVal(resType, ans);
            }
            case LSS -> {
                ans = l < r ?"true":"false";
                resType = new ValueType(ValueType.IRType.INT1, false);
                return new ConstVal(resType, ans);
            }
            case GEQ -> {
                ans = l >= r ?"true":"false";
                resType = new ValueType(ValueType.IRType.INT1, false);
                return new ConstVal(resType, ans);
            }
            case LEQ -> {
                ans = l <= r ?"true":"false";
                resType = new ValueType(ValueType.IRType.INT1, false);
                return new ConstVal(resType, ans);
            }
            case EQL -> {
                ans = l == r ?"true":"false";
                resType = new ValueType(ValueType.IRType.INT1, false);
                return new ConstVal(resType, ans);
            }
            case NEQ -> {
                ans = l != r ?"true":"false";
                resType = new ValueType(ValueType.IRType.INT1, false);
                return new ConstVal(resType, ans);
            }
        }
        ans = String.valueOf(res);
        return new ConstVal(resType, ans);
    }

    private ConstVal unaryCalConst(Value val, Tokens.Token op) {
        assert val instanceof ConstVal;
        ValueType type = val.getType();
        String ans = null;
        if (type.isType(ValueType.IRType.INT32, 0)) {
            int v = Integer.parseInt(((ConstVal) val).getValue());
            int res = 0;
            switch (op.getKind()) {
                case PLUS -> res = v;
                case MINU -> res = -v;
                case NOT -> {
                    return new ConstVal(new ValueType(ValueType.IRType.INT1, false), v == 0 ? "true" : "false");
                }
            }
            ans = String.valueOf(res);
        } else if (type.isType(ValueType.IRType.INT1, 0)) {
            switch (op.getKind()) {
                case NOT -> {
                    String v = ((ConstVal) val).getValue();
                    ans = v.equals("true") ? "false" : "true";
                }
                default -> System.out.println("unaryCalConst: error2");
            }
        }
        return new ConstVal(type, ans);
    }

    private ConstVal defaultInit(ValueType type) {
        ConstVal value = null;
        if (type.isType(ValueType.IRType.INT32, 0)) {
            value = new ConstVal(type, "0");
        }
        return value;
    }

    private Value int1Totype(Value value) {
        if (value.getType().isType(ValueType.IRType.INT1, 0)) {
            if (value instanceof ConstVal) {
                return ((ConstVal) value).i1Totype(new ValueType(ValueType.IRType.INT32, false));
            } else {
                return builder.buildZextInst(value, new ValueType(ValueType.IRType.INT32, false));
            }
        } else return value;
    }

    private Value TypeToInt1(Value value) {
        if (value.getType().isType(ValueType.IRType.INT32, 0)) {
            if (value instanceof ConstVal) {
                return ((ConstVal) value).Type2I1();
            } else return builder.buildBinaryInst(value, defaultInit(value.getType()), Tokens.TokenKind.NEQ);
        } else return value;
    }

}
