package exception.symbolTable;

import frontend.Tokens;

public enum Type {
    INT,
    VOID;

    public static Type toType(Tokens.Token type) {
        Type ret = null;
        switch (type.getKind()) {
            case INT -> ret = INT;
            case VOID -> ret = VOID;
        }
        if (ret == null) {
            System.out.println("token " + type.getTag() + " is not a type");
        }
        return ret;
    }
}
