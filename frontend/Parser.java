package frontend;

import exception.ExceptionHandler;
import exception.SysYException;
import frontend.tree.Number;
import frontend.tree.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Parser {
    private final Scanner scanner;
    private final ExceptionHandler exceptionHandler;
    private final BufferedWriter bufferedWriter;
    public CompUnit compUnit;

    public Parser(Scanner scanner, ExceptionHandler exceptionHandler, BufferedWriter bw) {
        this.scanner = scanner;
        this.exceptionHandler = exceptionHandler;
        this.bufferedWriter = bw;
    }

    private int getTokenLine(Unit token) {
        //;前无任何东西的情况
        if (token instanceof BasicNode) {
            return getTokenLine(((BasicNode) token).derivations.get(((BasicNode) token).derivations.size() - 1));
        } else
            return ((Tokens.Token) token).getLineLocated();
    }

    private boolean isBType(Tokens.TokenKind kind) {
        switch (kind) {
            case INT:
                return true;
        }
        return false;
    }

    private boolean isNumber(Tokens.TokenKind kind) {
        switch (kind) {
            case INTCONST -> {
                return true;
            }
        }
        return false;
    }

    private boolean isUnaryOp(Tokens.TokenKind kind) {
        switch (kind) {
            case PLUS, MINU, NOT -> {
                return true;
            }
        }
        return false;
    }

    private boolean checkCurTokenKind(Tokens.TokenKind kind) {
        return scanner.getTokenKind() == kind;
    }


    private void putAfterAssert(ArrayList<Unit> nodes, Tokens.Token token, Tokens.TokenKind kind) throws SysYException {
        if (token.getKind() != kind) {
            if (kind == Tokens.TokenKind.SEMICN) {
                int line = getTokenLine(nodes.get(nodes.size() - 1));
                nodes.add(new Tokens.Token(Tokens.TokenKind.SEMICN, -1));
                exceptionHandler.addError(new SysYException(SysYException.ExceptionKind.MISSING_SEMICOLON, line));
            } else if (kind == Tokens.TokenKind.RPARENT) {
                int line = getTokenLine(nodes.get(nodes.size() - 1));
                nodes.add(new Tokens.Token(Tokens.TokenKind.RPARENT, -1));
                exceptionHandler.addError(new SysYException(SysYException.ExceptionKind.MISSING_RIGHT_PARENT, line));
            } else if (kind == Tokens.TokenKind.RBRACK) {
                int line = getTokenLine(nodes.get(nodes.size() - 1));
                nodes.add(new Tokens.Token(Tokens.TokenKind.RBRACK, -1));
                exceptionHandler.addError(new SysYException(SysYException.ExceptionKind.MISSING_RIGHT_BRACKET, line));
            } else {
                System.out.println("[Parser] assert:" + kind.getTag() + " but meet " + token.getKind().getName());
                throw new SysYException(SysYException.ExceptionKind.ERROR, token.getLineLocated(), nodes);
            }
        } else {
            nodes.add(token);
            scanner.next();
        }
    }

    private void putBTypeAfterAssert(ArrayList<Unit> nodes, Tokens.Token token) throws SysYException {
        if (isBType(token.getKind())) {
            nodes.add(token);
            scanner.next();
        } else {
            System.out.println("[Parser] assert:" + " BType" + " but meet " + token.getKind().getName());
            throw new SysYException(SysYException.ExceptionKind.ERROR, token.getLineLocated(), nodes);
        }
    }

    private void putNumberAfterAssert(ArrayList<Unit> nodes, Tokens.Token token) throws SysYException {
        if (isNumber(token.getKind())) {
            nodes.add(token);
            scanner.next();
        } else {
            System.out.println("[Parser] assert:" + " Number" + " but meet " + token.getKind().getName());
            throw new SysYException(SysYException.ExceptionKind.ERROR, token.getLineLocated(), nodes);
        }
    }

    private void putWithoutAssert(ArrayList<Unit> nodes, Tokens.Token token) {
        nodes.add(token);
        scanner.next();
    }

    public CompUnit syntaxAnalyse(boolean print) throws IOException {
        try {
            compUnit = parseCompUnit();
        } catch (SysYException e) {
            System.out.println(e.record);
            System.out.println(e);
        }
        return compUnit;
    }

    public void printSyntaxTree() throws IOException {
        bufferedWriter.write(compUnit.toString());
        bufferedWriter.newLine();
        bufferedWriter.close();
    }

    private void postOrderTraversalSyntaxTree(Unit root) throws IOException {
        if (root != null) {
            if (root instanceof BasicNode) {
                for (Unit unit : ((BasicNode) root).derivations) {
                    postOrderTraversalSyntaxTree(unit);
                }
            }
            //System.out.println(root);
            bufferedWriter.write(root.toString());
            bufferedWriter.newLine();
        }
    }

    private CompUnit parseCompUnit() throws SysYException {
        scanner.next();
        ArrayList<Unit> nodes = new ArrayList<>();
        while (true) {
            if (scanner.getTokenKind() == Tokens.TokenKind.CONST) {
                nodes.add(parseConstDecl());
            } else if (scanner.getTokenKind() == Tokens.TokenKind.VOID) {
                nodes.add(parseFuncDef());
            } else if (isBType(scanner.getTokenKind())) {
                if (scanner.lookAhead(1).getKind() == Tokens.TokenKind.IDENT) {
                    if (scanner.lookAhead(2).getKind() == Tokens.TokenKind.LPARENT) {
                        nodes.add(parseFuncDef());
                    } else {
                        nodes.add(parseVarDecl());
                    }
                } else if (scanner.lookAhead(1).getKind() == Tokens.TokenKind.MAIN) {
                    nodes.add(parseMainFunDef());
                    break;
                }
            }
        }
        return new CompUnit(nodes);
    }

    private ConstDecl parseConstDecl() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.CONST);
        putBTypeAfterAssert(nodes, scanner.getToken());
        nodes.add(parseConstDef());
        while (scanner.getTokenKind() == Tokens.TokenKind.COMMA) {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.COMMA);
            nodes.add(parseConstDef());
        }
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
        return new ConstDecl(nodes);
    }

    private ConstDef parseConstDef() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.IDENT);
        while (scanner.getTokenKind() == Tokens.TokenKind.LBRACK) {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LBRACK);
            nodes.add(parseConstExp());
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RBRACK);
        }
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.ASSIGN);
        nodes.add(parseConstInitVal());
        return new ConstDef(nodes);
    }

    private ConstInitVal parseConstInitVal() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        if (scanner.getTokenKind() == Tokens.TokenKind.LBRACE) {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LBRACE);
            if (isTokenUnaryExpFirst(scanner.getToken())) {
                nodes.add(parseConstInitVal());
                while (scanner.getTokenKind() == Tokens.TokenKind.COMMA) {
                    putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.COMMA);
                    nodes.add(parseConstInitVal());
                }
            }
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RBRACE);
        } else {
            nodes.add(parseConstExp());
        }
        return new ConstInitVal(nodes);
    }

    private VarDecl parseVarDecl() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        putBTypeAfterAssert(nodes, scanner.getToken());
        nodes.add(parseVarDef());
        while (scanner.getTokenKind() == Tokens.TokenKind.COMMA) {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.COMMA);
            nodes.add(parseVarDef());
        }
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
        return new VarDecl(nodes);
    }

    private VarDef parseVarDef() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.IDENT);
        while (scanner.getTokenKind() == Tokens.TokenKind.LBRACK) {
            putWithoutAssert(nodes, scanner.getToken());
            nodes.add(parseConstExp());
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RBRACK);
        }
        if (scanner.getTokenKind() == Tokens.TokenKind.ASSIGN) {
            putWithoutAssert(nodes, scanner.getToken());
            nodes.add(parseInitVal());
        }
        return new VarDef(nodes);
    }

    private ConstExp parseConstExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseAddExp());
        return new ConstExp(nodes);
    }

    private AddExp parseAddExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseMulExp());
        while (true) {
            if (scanner.getTokenKind() == Tokens.TokenKind.PLUS || scanner.getTokenKind() == Tokens.TokenKind.MINU) {
                ArrayList<Unit> tnodes = new ArrayList<>(nodes);
                nodes.clear();
                nodes.add(new AddExp(tnodes));
                putWithoutAssert(nodes, scanner.getToken());
                nodes.add(parseMulExp());
            } else {
                break;
            }

        }
        return new AddExp(nodes);
    }

    private MulExp parseMulExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseUnaryExp());
        while (true) {
            if (scanner.getTokenKind() == Tokens.TokenKind.MULT || scanner.getTokenKind() == Tokens.TokenKind.DIV || scanner.getTokenKind() == Tokens.TokenKind.MOD) {
                ArrayList<Unit> tnodes = new ArrayList<>(nodes);
                nodes.clear();
                nodes.add(new MulExp(tnodes));
                putWithoutAssert(nodes, scanner.getToken());
                nodes.add(parseUnaryExp());
            } else {
                break;
            }

        }
        return new MulExp(nodes);
    }

    private UnaryExp parseUnaryExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        if (checkCurTokenKind(Tokens.TokenKind.LPARENT) || isNumber(scanner.getTokenKind())) {
            nodes.add(parsePrimaryExp());
        } else if (checkCurTokenKind(Tokens.TokenKind.IDENT)) {
            if (scanner.lookAhead(1).getKind() == Tokens.TokenKind.LPARENT) {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.IDENT);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
                if (checkCurTokenKind(Tokens.TokenKind.IDENT) || checkCurTokenKind(Tokens.TokenKind.LPARENT)
                        || isNumber(scanner.getTokenKind()) || isUnaryOp(scanner.getTokenKind())) {
                    nodes.add(parseFuncRParams());
                }
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
            } else {
                nodes.add(parsePrimaryExp());
            }
        } else if (isUnaryOp(scanner.getTokenKind())) {
            nodes.add(parseUnaryOp());
            nodes.add(parseUnaryExp());
        }
        return new UnaryExp(nodes);
    }

    private UnaryOp parseUnaryOp() {
        ArrayList<Unit> nodes = new ArrayList<>();
        putWithoutAssert(nodes, scanner.getToken());
        return new UnaryOp(nodes);
    }

    private FuncRParams parseFuncRParams() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseExp());
        while (scanner.getTokenKind() == Tokens.TokenKind.COMMA) {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.COMMA);
            nodes.add(parseExp());
        }
        return new FuncRParams(nodes);
    }

    private Exp parseExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseAddExp());
        return new Exp(nodes);
    }

    private PrimaryExp parsePrimaryExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        if (isNumber(scanner.getTokenKind())) {
            nodes.add(parseNumber());
        } else {
            switch (scanner.getTokenKind()) {
                case LPARENT -> {
                    putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
                    nodes.add(parseExp());
                    putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
                }
                case IDENT -> nodes.add(parseLVal());
            }
        }

        return new PrimaryExp(nodes);
    }

    private Number parseNumber() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        putNumberAfterAssert(nodes, scanner.getToken());
        return new Number(nodes);
    }

    private LVal parseLVal() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.IDENT);
        while (scanner.getTokenKind() == Tokens.TokenKind.LBRACK) {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LBRACK);
            nodes.add(parseExp());
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RBRACK);
        }
        return new LVal(nodes);
    }

    private InitVal parseInitVal() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        if (scanner.getTokenKind() == Tokens.TokenKind.LBRACE) {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LBRACE);
            if (isTokenUnaryExpFirst(scanner.getToken())) {
                nodes.add(parseInitVal());
                while (scanner.getTokenKind() == Tokens.TokenKind.COMMA) {
                    putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.COMMA);
                    nodes.add(parseInitVal());
                }
            }
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RBRACE);
        } else {
            nodes.add(parseExp());
        }
        return new InitVal(nodes);
    }

    private FuncDef parseFuncDef() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseFuncType());
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.IDENT);
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
        if (isBType(scanner.getTokenKind())) {
            nodes.add(parseFuncFParams());
        }
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
        nodes.add(parseBlock());
        return new FuncDef(nodes);
    }

    private Block parseBlock() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LBRACE);
        while (scanner.getTokenKind() != Tokens.TokenKind.RBRACE) {
            if (isBType(scanner.getTokenKind())) {
                nodes.add(parseVarDecl());
            } else {
                switch (scanner.getTokenKind()) {
                    case CONST -> nodes.add(parseConstDecl());
                    default -> nodes.add(parseStmt());
                }
            }
        }
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RBRACE);
        return new Block(nodes);
    }

    private Stmt parseStmt() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        switch (scanner.getTokenKind()) {
            case LBRACE -> nodes.add(parseBlock());
            case IF -> {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.IF);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
                nodes.add(parseCond());
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
                nodes.add(parseStmt());
                if (scanner.getTokenKind() == Tokens.TokenKind.ELSE) {
                    putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.ELSE);
                    nodes.add(parseStmt());
                }
            }
            case FOR -> {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.FOR);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
                if (scanner.getTokenKind() != Tokens.TokenKind.SEMICN) {
                    nodes.add(parseForStmt());
                }
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
                if (scanner.getTokenKind() != Tokens.TokenKind.SEMICN) {
                    nodes.add(parseCond());
                }
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
                if (scanner.getTokenKind() != Tokens.TokenKind.RPARENT) {
                    nodes.add(parseForStmt());
                }
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
                nodes.add(parseStmt());
            }
            case WHILE -> {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.WHILE);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
                nodes.add(parseCond());
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
                nodes.add(parseStmt());
            }
            case DO -> {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.DO);
                nodes.add(parseStmt());
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.WHILE);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
                nodes.add(parseCond());
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
            }
            case BREAK -> {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.BREAK);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
            }
            case CONTINUE -> {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.CONTINUE);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
            }
            case RETURN -> {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RETURN);
                if (isTokenUnaryExpFirst(scanner.getToken())) {
                    nodes.add(parseExp());
                }
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
            }
            case PRINTF -> {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.PRINTF);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.FMTSTRING);
                while (scanner.getTokenKind() == Tokens.TokenKind.COMMA) {
                    putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.COMMA);
                    nodes.add(parseExp());
                }
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
            }
            case SEMICN -> putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
            default -> {
                nodes.add(parseExp());
                if (scanner.getTokenKind() == Tokens.TokenKind.ASSIGN) {
                    BasicNode node = (BasicNode) nodes.get(0);
                    while (!Objects.equals(node.name, "LVal")) {
                        node = (BasicNode) node.derivations.get(0);
                    }
                    nodes.set(0, node);
                    putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.ASSIGN);
                    if (scanner.getTokenKind() == Tokens.TokenKind.GETINT) {
                        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.GETINT);
                        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
                        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
                        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
                    } else {
                        nodes.add(parseExp());
                        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
                    }
                }
                else {
                    //System.out.println(nodes);
                    putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.SEMICN);
                }
            }
        }
        return new Stmt(nodes);
    }


    private ForStmt parseForStmt() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseLVal());
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.ASSIGN);
        nodes.add(parseExp());
        return new ForStmt(nodes);
    }

    private Cond parseCond() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseLOrExp());
        return new Cond(nodes);
    }

    private LOrExp parseLOrExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseLAndExp());
        while (true) {
            if (scanner.getTokenKind() == Tokens.TokenKind.OR) {
                ArrayList<Unit> tnodes = new ArrayList<>(nodes);
                nodes.clear();
                nodes.add(new LOrExp(tnodes));
                putWithoutAssert(nodes, scanner.getToken());
                nodes.add(parseLAndExp());
            } else {
                break;
            }
        }
        return new LOrExp(nodes);
    }

    private LAndExp parseLAndExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseEqExp());
        while (true) {
            if (scanner.getTokenKind() == Tokens.TokenKind.AND) {
                ArrayList<Unit> tnodes = new ArrayList<>(nodes);
                nodes.clear();
                nodes.add(new LAndExp(tnodes));
                putWithoutAssert(nodes, scanner.getToken());
                nodes.add(parseEqExp());
            } else {
                break;
            }
        }
        return new LAndExp(nodes);
    }

    private EqExp parseEqExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseRelExp());
        while (true) {
            if (scanner.getTokenKind() == Tokens.TokenKind.EQL || scanner.getTokenKind() == Tokens.TokenKind.NEQ) {
                ArrayList<Unit> tnodes = new ArrayList<>(nodes);
                nodes.clear();
                nodes.add(new EqExp(tnodes));
                putWithoutAssert(nodes, scanner.getToken());
                nodes.add(parseRelExp());
            } else {
                break;
            }
        }
        return new EqExp(nodes);
    }

    private RelExp parseRelExp() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseAddExp());
        while (true) {
            if (scanner.getTokenKind() == Tokens.TokenKind.LSS
                    || scanner.getTokenKind() == Tokens.TokenKind.LEQ
                    || scanner.getTokenKind() == Tokens.TokenKind.GRE
                    || scanner.getTokenKind() == Tokens.TokenKind.GEQ) {
                ArrayList<Unit> tnodes = new ArrayList<>(nodes);
                nodes.clear();
                nodes.add(new RelExp(tnodes));
                putWithoutAssert(nodes, scanner.getToken());
                nodes.add(parseAddExp());
            } else {
                break;
            }

        }
        return new RelExp(nodes);
    }

    private FuncFParams parseFuncFParams() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        nodes.add(parseFuncFParam());
        while (scanner.getTokenKind() == Tokens.TokenKind.COMMA) {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.COMMA);
            nodes.add(parseFuncFParam());
        }
        return new FuncFParams(nodes);
    }

    private FuncFParam parseFuncFParam() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        putBTypeAfterAssert(nodes, scanner.getToken());
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.IDENT);
        if (scanner.getTokenKind() == Tokens.TokenKind.LBRACK) {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LBRACK);
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RBRACK);
            while (scanner.getTokenKind() == Tokens.TokenKind.LBRACK) {
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LBRACK);
                nodes.add(parseConstExp());
                putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RBRACK);
            }
        }
        return new FuncFParam(nodes);
    }

    private FuncType parseFuncType() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        if (isBType(scanner.getTokenKind())) {
            putBTypeAfterAssert(nodes, scanner.getToken());
        } else {
            putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.VOID);
        }
        return new FuncType(nodes);
    }

    private MainFuncDef parseMainFunDef() throws SysYException {
        ArrayList<Unit> nodes = new ArrayList<>();
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.INT);
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.MAIN);
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.LPARENT);
        putAfterAssert(nodes, scanner.getToken(), Tokens.TokenKind.RPARENT);
        nodes.add(parseBlock());
        return new MainFuncDef(nodes);
    }

    private boolean isTokenUnaryExpFirst(Tokens.Token token) {
        if (isNumber(token.getKind()) || isUnaryOp(token.getKind())) return true;
        switch (token.getKind()) {
            case LBRACE, LPARENT, IDENT -> {
                return true;
            }
        }
        return false;
    }
}
