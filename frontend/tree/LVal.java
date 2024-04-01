package frontend.tree;

import frontend.Tokens;
import frontend.Unit;
import exception.symbolTable.SymbolTable;
import exception.symbolTable.VarSymbol;

import java.util.List;

public class LVal extends ExpNode {
    public LVal(List<Unit> units) {
        super("LVal", units);
    }

    public Tokens.Token getIdent() {
        return (Tokens.Token) derivations.get(0);
    }

    @Override
    public int getDimension(SymbolTable symbolTable) {
        if (dimension != -1) return dimension;
        Tokens.Token ident = (Tokens.Token) derivations.get(0);
        VarSymbol symbol = (VarSymbol) symbolTable.findSymbolInAll(ident.getValue());
        if (derivations.size() == 1) {
            return dimension = symbol.dimension;
        } else if (derivations.size() == 4) {
            return dimension = symbol.dimension - 1;
        } else {
            return dimension = symbol.dimension - 2;
        }
    }
}
