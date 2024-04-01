package exception;

import exception.symbolTable.*;
import frontend.Tokens;
import frontend.Unit;
import frontend.tree.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class ExceptionHandler {
    private final BufferedWriter bufferedWriter;
    private final List<SysYException> errors = new ArrayList<>();

    private final SymbolTable rootTable = new SymbolTable(null, false);
    private final Map<String, Block> funcBlocks = new HashMap<>();

    public ExceptionHandler(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    public void addError(SysYException e) {
        errors.add(e);
    }

    public int size() {
        //System.out.println(errors);
        return errors.size();
    }

    public void printErrors() throws IOException {
        Collections.sort(errors);
        //System.out.println(errors);
        for (SysYException e : errors) {
            bufferedWriter.write(e.toString());
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
    }


    public boolean checkSameKindToken(Unit unit, Tokens.TokenKind kind) {
        if (unit instanceof Tokens.Token) {
            return ((Tokens.Token) unit).getKind() == kind;
        }
        return false;
    }

    public boolean checkErrorB(Tokens.Token token, SymbolTable symbolTable) {
        if (token.getKind() != Tokens.TokenKind.IDENT)
            return false;

        if (symbolTable.findSymbol(token.getValue()) != null) {
            addError(new SysYException(SysYException.ExceptionKind.NAME_REDEFINITION, token.getLineLocated()));
            return true;
        } else
            return false;
    }

    public void checkErrorH(SymbolTable symbolTable, LVal lVal) {
        VarSymbol symbol = (VarSymbol) symbolTable.findSymbolInAll(lVal.getIdent().getValue());
        if (symbol.isConst) {
            addError(new SysYException(SysYException.ExceptionKind.MODIFY_CONST_VALUE, lVal.getIdent().getLineLocated()));
        }
    }


    public SymbolTable analyseError(CompUnit compUnit) {
        for (int i = 0; i < compUnit.derivations.size(); i++) {
            Unit unit = compUnit.derivations.get(i);
            if (unit instanceof ConstDecl) {
                analyseConstDecl(rootTable, (ConstDecl) unit);
            } else if (unit instanceof VarDecl) {
                analyseVarDecl(rootTable, (VarDecl) unit);
            } else if (unit instanceof FuncDef) {
                analyseFuncDef(rootTable, (FuncDef) unit);
            } else if (unit instanceof MainFuncDef) {
                analyseMainFuncDef(rootTable, (MainFuncDef) unit);
            }
        }
        return rootTable;
    }

    private void analyseMainFuncDef(SymbolTable rootTable, MainFuncDef mainFuncDef) {
        FuncSymbol main = new FuncSymbol("main", Type.INT, new ArrayList<>(), (Block) mainFuncDef.derivations.get(4));
        rootTable.addSymbol(main);
        analyseFuncReturn(main);
        SymbolTable mainTable = new SymbolTable(rootTable);
        analyseBlock(rootTable, mainTable, (Block) mainFuncDef.derivations.get(4));
    }

    private void analyseFuncReturn(FuncSymbol funcSymbol) {
        if (funcSymbol.retType == Type.VOID) {
            assert funcSymbol.block != null;
            analyseFuncBlockReturn(false, funcSymbol.block);
        } else {
            Block block = funcSymbol.block;
            //只要检查最后一条语句是不是有返回值的return即可～
            //System.out.println(block.derivations.get(block.derivations.size() - 2));
            Unit unit = block.derivations.get(block.derivations.size() - 2);
            if (unit instanceof Stmt) {
                if (((Stmt) unit).getType() == Stmt.StmtType.RETURN) {
                    for (Unit unit1 : ((Stmt) unit).derivations) {
                        if (unit1 instanceof Exp)
                            return;
                    }
                }
            }
            addError(new SysYException(SysYException.ExceptionKind.MISSING_RETURN_STATEMENT,
                    ((Tokens.Token) block.derivations.get(block.derivations.size() - 1)).getLineLocated()));
        }
    }

    private void analyseFuncBlockReturn(boolean haveReturn, Block block) {
        for (Unit unit : block.derivations) {
            if (unit instanceof Stmt) {
                if (((Stmt) unit).getType() == Stmt.StmtType.BLOCK) {
                    analyseFuncBlockReturn(haveReturn, (Block) ((Stmt) unit).derivations.get(0));
                } else if (((Stmt) unit).getType() == Stmt.StmtType.RETURN) {
                    Tokens.Token token = (Tokens.Token) ((Stmt) unit).derivations.get(0);
                    if (haveReturn) {
                        if (((Stmt) unit).derivations.size() != 3) {
                            //不需要检查这个错误QAQ
                        }
                    } else {
                        if (((Stmt) unit).derivations.size() > 2)
                            addError(new SysYException(SysYException.ExceptionKind.RETURN_TYPE_MISMATCH, token.getLineLocated()));
                    }
                }
            }
        }
    }

    private void analyseFuncDef(SymbolTable symbolTable, FuncDef funcDef) {
        int i = 0;
        Tokens.Token funcType = (Tokens.Token) ((FuncType) funcDef.derivations.get(i)).derivations.get(i);
        Type type = Type.toType(funcType);
        Tokens.Token ident = (Tokens.Token) funcDef.derivations.get(++i);
        i += 2;
        List<FuncFParam> params = new ArrayList<>();
        if (funcDef.derivations.get(i) instanceof FuncFParams) {
            params = analyseFuncParams(symbolTable, (FuncFParams) funcDef.derivations.get(i));
        }
        i += 2;

        if (!checkErrorB(ident, symbolTable)) {
            Block block = null;
            for (Unit unit : funcDef.derivations) {
                if (unit instanceof Block)
                    block = (Block) unit;
            }
            FuncSymbol symbol = new FuncSymbol(ident.getValue(), type, params, block);
            symbolTable.addSymbol(symbol);
            analyseFuncReturn(symbol);

            //进入函数内部
            SymbolTable funcTable = new SymbolTable(symbolTable, false);
            //声明形参
            if (params != null) {
                for (FuncFParam param : symbol.params) {
                    if (!checkErrorB(param.getIdent(), funcTable))
                        funcTable.addSymbol(new VarSymbol(param.getIdent().getValue(), param.getDimension(), false));
                }
            }

            //检查block
            analyseBlock(symbolTable, funcTable, ((FuncSymbol) symbol).block);
        }

    }

    private List<FuncFParam> analyseFuncParams(SymbolTable symbolTable, FuncFParams funcFParams) {
        List<FuncFParam> list = new ArrayList<>();
        for (int i = 0; i < funcFParams.derivations.size(); i++) {
            Unit unit = funcFParams.derivations.get(i);
            if (unit instanceof FuncFParam) {
                analyseFuncFParam(symbolTable, (FuncFParam) unit);
                list.add((FuncFParam) unit);
            }
        }
        return list;
    }

    private void analyseFuncFParam(SymbolTable symbolTable, FuncFParam funcFParam) {
        //mark
        if (size() == 7) {
            analyseConstExp(symbolTable, (ConstExp) funcFParam.derivations.get(5));
        }
    }

    private void analyseVarDecl(SymbolTable symbolTable, VarDecl varDecl) {
        for (int i = 0; i < varDecl.derivations.size(); i++) {
            Unit unit = varDecl.derivations.get(i);
            if (unit instanceof VarDef)
                analyseVarDef(symbolTable, (VarDef) unit);
        }
    }

    private void analyseVarDef(SymbolTable symbolTable, VarDef varDef) {
        int i = 0;
        int dimension = 0;
        Tokens.Token ident = null;

        for (Unit unit : varDef.derivations) {
            if (unit instanceof Tokens.Token && ((Tokens.Token) unit).getKind() == Tokens.TokenKind.IDENT) {
                ident = (Tokens.Token) unit;
            }
            if (unit instanceof ConstExp) {
                analyseConstExp(symbolTable, (ConstExp) unit);
                dimension++;
            }
            if (unit instanceof InitVal) {
                analyseInitVal(symbolTable, (InitVal) unit);
            }
        }

        assert ident != null;
        if (!checkErrorB(ident, symbolTable)) {
            symbolTable.addSymbol(new VarSymbol(ident.getValue(), dimension, false));
        }
    }

    private void analyseInitVal(SymbolTable symbolTable, InitVal initVal) {
        if (initVal.derivations.get(0) instanceof Exp) {
            analyseExp(symbolTable, (Exp) initVal.derivations.get(0));
        } else {
            for (int i = 0; i < initVal.derivations.size(); i++) {
                if (initVal.derivations.get(i) instanceof InitVal)
                    analyseInitVal(symbolTable, (InitVal) initVal.derivations.get(i));
            }
        }
    }

    private void analyseExp(SymbolTable symbolTable, Exp exp) {
        analyseAddExp(symbolTable, (AddExp) exp.derivations.get(0));
    }

    private void analyseConstDecl(SymbolTable symbolTable, ConstDecl constDecl) {
        for (int i = 0; i < constDecl.derivations.size(); i++) {
            Unit unit = constDecl.derivations.get(i);
            if (unit instanceof ConstDef)
                analyseConstDef(symbolTable, (ConstDef) unit);
        }
    }

    private void analyseConstDef(SymbolTable symbolTable, ConstDef constDef) {
        int i = 0;
        int dimension = 0;
        Tokens.Token ident = null;

        for (Unit unit : constDef.derivations) {
            if (unit instanceof Tokens.Token && ((Tokens.Token) unit).getKind() == Tokens.TokenKind.IDENT) {
                ident = (Tokens.Token) unit;
            }
            if (unit instanceof ConstExp) {
                analyseConstExp(symbolTable, (ConstExp) unit);
                dimension++;
            }
            if (unit instanceof ConstInitVal) {
                analyseConstInitVal(symbolTable, (ConstInitVal) unit);
            }
        }

        assert ident != null;
        if (!checkErrorB(ident, symbolTable)) {
            symbolTable.addSymbol(new VarSymbol(ident.getValue(), dimension, true));
        }
    }

    private void analyseConstInitVal(SymbolTable symbolTable, ConstInitVal constInitVal) {
        //ConstInitVal 中的 ConstExp 是能在编译时求值的 int 型表达式，其中可以引用已定义的符号常量。
        if (constInitVal.derivations.get(0) instanceof ConstExp) {
            analyseConstExp(symbolTable, (ConstExp) constInitVal.derivations.get(0));
        } else {
            for (int i = 0; i < constInitVal.derivations.size(); i++) {
                if (constInitVal.derivations.get(i) instanceof ConstInitVal)
                    analyseConstInitVal(symbolTable, (ConstInitVal) constInitVal.derivations.get(i));
            }
        }
    }

    private void analyseConstExp(SymbolTable symbolTable, ConstExp constExp) {
        analyseAddExp(symbolTable, (AddExp) constExp.derivations.get(0));
    }

    private void analyseAddExp(SymbolTable symbolTable, AddExp addExp) {
        //因为没说，先不检查ConstExp和Exp的区分～
        for (int i = 0; i < addExp.derivations.size(); i++) {
            if (addExp.derivations.get(i) instanceof AddExp)
                analyseAddExp(symbolTable, (AddExp) addExp.derivations.get(i));
            if (addExp.derivations.get(i) instanceof MulExp)
                analyseMulExp(symbolTable, (MulExp) addExp.derivations.get(i));
        }
    }

    private void analyseMulExp(SymbolTable symbolTable, MulExp mulExp) {
        for (int i = 0; i < mulExp.derivations.size(); i++) {
            if (mulExp.derivations.get(i) instanceof MulExp)
                analyseMulExp(symbolTable, (MulExp) mulExp.derivations.get(i));
            if (mulExp.derivations.get(i) instanceof UnaryExp)
                analyseUnaryExp(symbolTable, (UnaryExp) mulExp.derivations.get(i));
        }
    }

    //默认没有这种错误：int+void 或者 int + int[]
    private void analyseUnaryExp(SymbolTable symbolTable, UnaryExp unaryExp) {
        Unit unit = unaryExp.derivations.get(0);
        //unaryExp.debug();
        if (unit instanceof PrimaryExp)
            analysePrimaryExp(symbolTable, (PrimaryExp) unit);
        else if (unit instanceof UnaryOp) {
            analyseUnaryExp(symbolTable, (UnaryExp) unaryExp.derivations.get(1));
        } else {
            //函数调用
            Tokens.Token ident = (Tokens.Token) unit;
            //System.out.println(ident.getValue()+" "+ident.getLineLocated());
            Symbol symbol = symbolTable.findSymbolInAll(ident.getValue());
            //是否定义
            //1.定义
            if (symbol instanceof FuncSymbol) {
                //有无传参
                //System.out.println("analyseUnaryExp:"+symbol.name+" define="+((FuncSymbol) symbol).params.size()+ " in="+((FuncRParams) unaryExp.derivations.get(2)).getExps().size());
                //1.有
                if (unaryExp.derivations.size() > 2 && unaryExp.derivations.get(2) instanceof FuncRParams) {
                    List<Exp> expList = ((FuncRParams) unaryExp.derivations.get(2)).getExps();
                    //参数数量不匹配，返回报错d
                    if (expList.size() != ((FuncSymbol) symbol).params.size()) {
                        addError(new SysYException(SysYException.ExceptionKind.PARAM_COUNT_MISMATCH, ident.getLineLocated()));
                        return;
                    }
                    //遍历每个参数
                    for (int i = 0; i < expList.size(); i++) {
                        analyseExp(symbolTable, expList.get(i));
                        if (((FuncSymbol) symbol).params.get(i).getDimension() != expList.get(i).getDimension(symbolTable)) {
                            System.out.println("expected: "+((FuncSymbol) symbol).params.get(i).getDimension()+ " meet "+ expList.get(i).getDimension(symbolTable));
                            addError(new SysYException(SysYException.ExceptionKind.PARAM_TYPE_MISMATCH, ident.getLineLocated()));
                            return;
                        }
                    }
                } else//2.无
                {
                    if (!((FuncSymbol) symbol).params.isEmpty())
                        addError(new SysYException(SysYException.ExceptionKind.PARAM_COUNT_MISMATCH, ident.getLineLocated()));
                }

            }//2.未定义，报c
            else
                addError(new SysYException(SysYException.ExceptionKind.UNDEFINED_NAME, ident.getLineLocated()));
        }
    }

    private void analyseBlock(SymbolTable father, SymbolTable symbolTable, Block block) {
        for (Unit unit : block.derivations) {
            if (unit instanceof VarDecl)
                analyseVarDecl(symbolTable, (VarDecl) unit);
            else if (unit instanceof ConstDecl) {
                analyseConstDecl(symbolTable, (ConstDecl) unit);
            } else if (unit instanceof Stmt) {
                analyseStmt(symbolTable, (Stmt) unit);
            }
        }
    }

    private void analyseStmt(SymbolTable symbolTable, Stmt stmt) {
        switch (stmt.getType()) {
            case BLOCK -> {
                SymbolTable blockTable = new SymbolTable(symbolTable);
                analyseBlock(symbolTable, blockTable, (Block) stmt.derivations.get(0));
            }
            case IF -> {
                analyseCond(symbolTable, (Cond) stmt.derivations.get(2));
                Stmt stmt1 = (Stmt) stmt.derivations.get(4);
                Stmt stmt2 = null;
                if (stmt.derivations.size() > 5) {
                    stmt2 = (Stmt) stmt.derivations.get(6);
                }
                if (stmt1.getType() == Stmt.StmtType.BLOCK) {
                    SymbolTable blockTable = new SymbolTable(symbolTable);
                    analyseBlock(symbolTable, blockTable, (Block) stmt1.derivations.get(0));
                } else {
                    SymbolTable forTable = new SymbolTable(symbolTable);
                    analyseStmt(forTable, stmt1);
                }
                if (stmt2 != null) {
                    if (stmt2.getType() == Stmt.StmtType.BLOCK) {
                        SymbolTable blockTable = new SymbolTable(symbolTable);
                        analyseBlock(symbolTable, blockTable, (Block) stmt2.derivations.get(0));
                    } else {
                        SymbolTable forTable = new SymbolTable(symbolTable);
                        analyseStmt(forTable, stmt2);
                    }
                }
            }
            case FOR -> {
                for (Unit unit : stmt.derivations) {
                    if (unit instanceof ForStmt)
                        analyseForStmt(symbolTable, (ForStmt) unit);
                    if (unit instanceof Cond)
                        analyseCond(symbolTable, (Cond) unit);
                    if (unit instanceof Stmt) {
                        if (((Stmt) unit).getType() == Stmt.StmtType.BLOCK) {
                            SymbolTable blockTable = new SymbolTable(symbolTable, true);
                            analyseBlock(symbolTable, blockTable, (Block) ((Stmt) unit).derivations.get(0));
                        } else {
                            SymbolTable forTable = new SymbolTable(symbolTable, true);
                            analyseStmt(forTable, (Stmt) unit);
                        }

                    }
                }
            }
            case WHILE,DO_WHILE ->{
                for (Unit unit : stmt.derivations) {
                    if (unit instanceof Cond)
                        analyseCond(symbolTable, (Cond) unit);
                    if (unit instanceof Stmt) {
                        if (((Stmt) unit).getType() == Stmt.StmtType.BLOCK) {
                            SymbolTable blockTable = new SymbolTable(symbolTable, true);
                            analyseBlock(symbolTable, blockTable, (Block) ((Stmt) unit).derivations.get(0));
                        } else {
                            SymbolTable forTable = new SymbolTable(symbolTable, true);
                            analyseStmt(forTable, (Stmt) unit);
                        }

                    }
                }
            }
            case BREAK, CONTINUE -> {
                if (!symbolTable.isInLoop())
                    addError(new SysYException(SysYException.ExceptionKind.ERROR_BREAK_CONTINUE,
                            ((Tokens.Token) stmt.derivations.get(0)).getLineLocated()));
            }
            case RETURN -> {
                //return的匹配情况会用别的函数专门检查，所以这里仅检查exp的合法性
                for (Unit unit : stmt.derivations) {
                    if (unit instanceof Exp) {
                        analyseExp(symbolTable, (Exp) unit);
                    }
                }
            }
            case PRINTF -> {
                int num = checkFmt((Tokens.Token) stmt.derivations.get(2));
                int expCnt = 0;
                for (int i = 3; i < stmt.derivations.size(); i++) {
                    if (stmt.derivations.get(i) instanceof Exp) {
                        analyseExp(symbolTable, (Exp) stmt.derivations.get(i));
                        expCnt++;
                    }
                }
                if (num != -1 && expCnt != num) {
                    addError(new SysYException(SysYException.ExceptionKind.PRINTF_ARG_MISMATCH,
                            ((Tokens.Token) stmt.derivations.get(0)).getLineLocated()));
                }
            }
            case GETINT -> {
                LVal lVal = (LVal) stmt.derivations.get(0);
                if (analyseLVal(symbolTable, lVal))
                    checkErrorH(symbolTable, lVal);
            }
            case LVAL -> {
                LVal lVal = (LVal) stmt.derivations.get(0);
                if (analyseLVal(symbolTable, lVal))
                    checkErrorH(symbolTable, lVal);
                Exp exp = (Exp) stmt.derivations.get(2);
                analyseExp(symbolTable, exp);
            }
            case EXP -> {
                if (stmt.derivations.get(0) instanceof Exp)
                    analyseExp(symbolTable, (Exp) stmt.derivations.get(0));
            }
        }
    }

    private void analyseForStmt(SymbolTable symbolTable, ForStmt forStmt) {
        LVal lVal = (LVal) forStmt.derivations.get(0);
        if (analyseLVal(symbolTable, lVal))
            checkErrorH(symbolTable, lVal);
        analyseExp(symbolTable, (Exp) forStmt.derivations.get(2));
    }

    private void analyseCond(SymbolTable symbolTable, Cond cond) {
        analyseLOrExp(symbolTable, (LOrExp) cond.derivations.get(0));
    }

    private void analyseLOrExp(SymbolTable symbolTable, LOrExp lOrExp) {
        for (int i = 0; i < lOrExp.derivations.size(); i++) {
            if (lOrExp.derivations.get(i) instanceof LOrExp)
                analyseLOrExp(symbolTable, (LOrExp) lOrExp.derivations.get(i));
            if (lOrExp.derivations.get(i) instanceof LAndExp)
                analyseLAndExp(symbolTable, (LAndExp) lOrExp.derivations.get(i));
        }
    }

    private void analyseLAndExp(SymbolTable symbolTable, LAndExp lAndExp) {
        for (int i = 0; i < lAndExp.derivations.size(); i++) {
            if (lAndExp.derivations.get(i) instanceof LAndExp)
                analyseLAndExp(symbolTable, (LAndExp) lAndExp.derivations.get(i));
            if (lAndExp.derivations.get(i) instanceof EqExp)
                analyseEqExp(symbolTable, (EqExp) lAndExp.derivations.get(i));
        }
    }

    private void analyseEqExp(SymbolTable symbolTable, EqExp eqExp) {
        for (int i = 0; i < eqExp.derivations.size(); i++) {
            if (eqExp.derivations.get(i) instanceof EqExp)
                analyseEqExp(symbolTable, (EqExp) eqExp.derivations.get(i));
            if (eqExp.derivations.get(i) instanceof RelExp)
                analyseRelExp(symbolTable, (RelExp) eqExp.derivations.get(i));
        }
    }

    private void analyseRelExp(SymbolTable symbolTable, RelExp relExp) {
        for (int i = 0; i < relExp.derivations.size(); i++) {
            if (relExp.derivations.get(i) instanceof RelExp)
                analyseRelExp(symbolTable, (RelExp) relExp.derivations.get(i));
            if (relExp.derivations.get(i) instanceof AddExp)
                analyseAddExp(symbolTable, (AddExp) relExp.derivations.get(i));
        }
    }

    private void analysePrimaryExp(SymbolTable symbolTable, PrimaryExp primaryExp) {
        Unit unit = primaryExp.derivations.get(0);
        if (unit instanceof LVal) {
            analyseLVal(symbolTable, (LVal) unit);
        } else {
            if (checkSameKindToken(unit, Tokens.TokenKind.LPARENT)) {
                analyseExp(symbolTable, (Exp) primaryExp.derivations.get(1));
            }
        }
    }

    private boolean analyseLVal(SymbolTable symbolTable, LVal lVal) {
        Tokens.Token ident;
        boolean ret = true;
        for (Unit unit : lVal.derivations) {
            if (unit instanceof Tokens.Token && ((Tokens.Token) unit).getKind() == Tokens.TokenKind.IDENT) {
                ident = (Tokens.Token) unit;
                if (symbolTable.findSymbolInAll(ident.getValue()) == null) {
                    addError(new SysYException(SysYException.ExceptionKind.UNDEFINED_NAME, ident.getLineLocated()));
                    ret = false;
                }
            }
            if (unit instanceof Exp)
                analyseExp(symbolTable, (Exp) unit);
        }
        return ret;
    }

    private int checkFmt(Tokens.Token fmt) {
        if (fmt.getKind() != Tokens.TokenKind.FMTSTRING)
            return -1;
        char[] val = fmt.getValue().toCharArray();
        int cnt = 0;
        for (int i = 1; i < fmt.getValue().length() - 1; i++) {
            if (val[i] == 37) {
                if (i + 1 == fmt.getValue().length() - 1 || val[i + 1] != 100) {
                    addError(new SysYException(SysYException.ExceptionKind.ILLEGAL_SYMBOL, fmt.getLineLocated()));
                    return -1;
                }
                i++;
                cnt++;
            } else if (val[i] == 92) {
                if (i + 1 == fmt.getValue().length() - 1 || val[i + 1] != 110) {
                    addError(new SysYException(SysYException.ExceptionKind.ILLEGAL_SYMBOL, fmt.getLineLocated()));
                    return -1;
                }
                i++;
            } else if (!(val[i] == 32 || val[i] == 33 || (val[i] >= 40 && val[i] <= 126))) {
                addError(new SysYException(SysYException.ExceptionKind.ILLEGAL_SYMBOL, fmt.getLineLocated()));
                return -1;
            }
        }
        return cnt;
    }

}
