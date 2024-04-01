package frontend.tree;

import frontend.Unit;

import java.util.ArrayList;
import java.util.List;

public class FuncRParams extends BasicNode {
    public FuncRParams(List<Unit> units) {
        super("FuncRParams", units);
    }

    public List<Exp> getExps() {
        List<Exp> list = new ArrayList<>();
        for (Unit unit : derivations) {
            if (unit instanceof Exp) {
                list.add((Exp) unit);
            }
        }
        return list;
    }
}
