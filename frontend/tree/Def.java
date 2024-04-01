package frontend.tree;

import frontend.Unit;

import java.util.ArrayList;
import java.util.List;

public class Def extends BasicNode {
    public Def(String name, List<Unit> units) {
        super(name, units);
    }

    public List<ConstExp> getDimensions() {
        List<ConstExp> dimensions = new ArrayList<>();
        for (Unit unit : derivations) {
            if (unit instanceof ConstExp constExp) {
                dimensions.add(constExp);
            }
        }
        return dimensions;
    }

    public BasicInitVal getInitVal() {
        BasicInitVal initVal = null;
        for (Unit unit : derivations) {
            if (unit instanceof BasicInitVal) {
                initVal = (BasicInitVal) unit;
            }
        }
        return initVal;
    }
}
