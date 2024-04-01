package frontend.tree;

import frontend.Tokens;
import frontend.Unit;

import java.util.List;

public class Stmt extends BasicNode {
    public Stmt(List<Unit> units) {
        super("Stmt", units);
    }

    public StmtType getType() {
        int i = 0;
        if (derivations.get(i) instanceof Block)
            return StmtType.BLOCK;
        if (derivations.get(i) instanceof Tokens.Token) {
            switch (((Tokens.Token) derivations.get(i)).getKind()) {
                case IF -> {
                    return StmtType.IF;
                }
                case FOR -> {
                    return StmtType.FOR;
                }
                case WHILE -> {
                    return StmtType.WHILE;
                }
                case DO -> {
                    return StmtType.DO_WHILE;
                }
                case BREAK -> {
                    return StmtType.BREAK;
                }
                case CONTINUE -> {
                    return StmtType.CONTINUE;
                }
                case RETURN -> {
                    return StmtType.RETURN;
                }
                case PRINTF -> {
                    return StmtType.PRINTF;
                }
            }
        }
        if (derivations.get(i) instanceof LVal) {
            if (derivations.get(2) instanceof Exp)
                return StmtType.LVAL;
            else return StmtType.GETINT;
        }
        return StmtType.EXP;
    }

    public enum StmtType {
        BLOCK,
        IF,
        FOR,
        WHILE,
        DO_WHILE,
        BREAK,
        CONTINUE,
        RETURN,
        PRINTF,
        LVAL,
        EXP,
        GETINT;
    }
}
