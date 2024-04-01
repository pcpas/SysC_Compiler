package frontend.tree;

import frontend.Tokens;
import frontend.Unit;
import exception.symbolTable.FuncSymbol;
import exception.symbolTable.SymbolTable;
import exception.symbolTable.Type;

import java.util.List;

public class UnaryExp extends ExpNode {
    public UnaryExp(List<Unit> units) {
        super("UnaryExp", units);
    }

    @Override
    public int getDimension(SymbolTable symbolTable) {
        if (dimension != -1) return dimension;
        Unit unit = derivations.get(0);
        if (unit instanceof Tokens.Token) {
            //如果是函数调用
            FuncSymbol funcSymbol = (FuncSymbol) symbolTable.findSymbolInAll(((Tokens.Token) unit).getValue());
            if (funcSymbol.retType == Type.INT)
                return dimension = 0;
            else
                return -1;
        } else if (unit instanceof PrimaryExp) {
            return dimension = ((PrimaryExp) unit).getDimension(symbolTable);
        } else
            return dimension = ((UnaryExp) derivations.get(1)).getDimension(symbolTable);
    }
}
