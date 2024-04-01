package frontend.tree;

import frontend.Unit;

import java.util.List;

public class FuncDef extends BasicNode {
    public FuncDef(List<Unit> units) {
        super("FuncDef", units);
    }

    public FuncFParams getFuncFParams() {
        Unit unit = derivations.get(3);
        if (unit instanceof FuncFParams) return (FuncFParams) unit;
        else return null;
    }

    public Block getBlock() {
        Unit unit = derivations.get(4);
        if (unit instanceof Block) return (Block) unit;
        else return (Block) derivations.get(5);
    }

}
