package frontend.tree;

import frontend.Unit;

import java.util.List;

public class VarDef extends Def {

    public VarDef(List<Unit> units) {
        super("VarDef", units);
    }

    @Override
    public InitVal getInitVal() {
        InitVal initVal = null;
        for (Unit unit : derivations) {
            if (unit instanceof InitVal) {
                initVal = (InitVal) unit;
            }
        }
        return initVal;
    }
}
