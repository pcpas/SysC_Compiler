package frontend.tree;

import frontend.Tokens;
import frontend.Unit;
import exception.symbolTable.SymbolTable;

import java.util.List;

public class PrimaryExp extends ExpNode {
    public PrimaryExp(List<Unit> units) {
        super("PrimaryExp", units);
    }

    @Override
    public int getDimension(SymbolTable symbolTable) {
        if (dimension != -1) return dimension;

        Unit unit = derivations.get(0);
        if (unit instanceof LVal) {
            return dimension = ((LVal) unit).getDimension(symbolTable);
        } else if (unit instanceof Number) {
            return 0;
        } else {
            if (unit instanceof Tokens.Token && ((Tokens.Token) unit).getKind() == Tokens.TokenKind.LPARENT) {
                return dimension = ((Exp) derivations.get(1)).getDimension(symbolTable);
            } else
                return -2;//出错了
        }
    }
}
