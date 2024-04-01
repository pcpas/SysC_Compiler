package frontend.tree;

import frontend.Unit;

import java.util.List;

public class ConstDef extends Def {
    public ConstDef(List<Unit> units) {
        super("ConstDef", units);
    }

    @Override
    public ConstInitVal getInitVal() {
        ConstInitVal initVal = null;
        for (Unit unit : derivations) {
            if (unit instanceof ConstInitVal) {
                initVal = (ConstInitVal) unit;
            }
        }
        return initVal;
    }
}
