package frontend;

/***
 * define tokens and provide specific methods
 */
public class Tokens {

    public boolean isExist(String tokenName) {
        for (TokenKind tokenKind : TokenKind.values()) {
            if (tokenKind.getName().equals(tokenName)) {
                return true;
            }
        }
        return false;
    }

    //define ALL kinds of token
    public enum TokenKind {
        IDENT("Ident", "IDENFR"),
        INTCONST("IntConst", "INTCON"),
        FMTSTRING("FormatString", "STRCON"),

        MAIN("main"),
        CONST("const"),
        INT("int"),
        BREAK("break"),
        CONTINUE("continue"),
        IF("if"),
        ELSE("else"),
        FOR("for"),
        DO("do"),
        WHILE("while"),
        RETURN("return"),
        GETINT("getint"),
        PRINTF("printf"),
        VOID("void"),

        NOT("!", "NOT"),
        AND("&&", "AND"),
        OR("||", "OR"),
        PLUS("+", "PLUS"),
        MINU("-", "MINU"),
        MULT("*", "MULT"),
        DIV("/", "DIV"),
        MOD("%", "MOD"),
        LSS("<", "LSS"),
        LEQ("<=", "LEQ"),
        GRE(">", "GRE"),
        GEQ(">=", "GEQ"),
        EQL("==", "EQL"),
        NEQ("!=", "NEQ"),
        ASSIGN("=", "ASSIGN"),
        SEMICN(";", "SEMICN"),
        COMMA(",", "COMMA"),
        LPARENT("(", "LPARENT"),
        RPARENT(")", "RPARENT"),
        LBRACK("[", "LBRACK"),
        RBRACK("]", "RBRACK"),
        LBRACE("{", "LBRACE"),
        RBRACE("}", "RBRACE");

        private final String name;
        private final String tag;

        TokenKind(String name) {
            this.name = name;
            this.tag = name.toUpperCase() + "TK";
        }

        TokenKind(String name, String tag) {
            this.name = name;
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }

        public String getName() {
            return name;
        }

        public String getTag() {
            return tag;
        }


    }

    public static class Token implements Unit {
        private TokenKind kind = null;
        private String identValue = null;
        private Integer lineLocated = null;


        public Token(String tokenName, Integer lineLocated) {
            //这里假设不存在不匹配的情况
            for (TokenKind tokenKind : TokenKind.values()) {
                if (tokenKind.getName().equals(tokenName)) {
                    this.kind = tokenKind;
                    break;
                }
            }
            this.lineLocated = lineLocated;
        }

        public Token(TokenKind kind, Integer lineLocated) {
            this.kind = kind;
            this.lineLocated = lineLocated;
        }

        public Token(TokenKind kind, String identValue, Integer lineLocated) {
            this.kind = kind;
            this.identValue = identValue;
            this.lineLocated = lineLocated;
        }

        @Override
        public String toString() {
            if (identValue == null) {
                return kind.tag + " " + kind.name;
            } else {
                return kind.tag + " " + identValue;
            }
        }

        public String getTag() {
            return kind.tag;
        }

        public TokenKind getKind() {
            return kind;
        }

        public String getValue() {
            if (identValue != null)
                return identValue;
            else
                return kind.name;
        }

        public Integer getLineLocated() {
            return lineLocated;
        }

    }
}
