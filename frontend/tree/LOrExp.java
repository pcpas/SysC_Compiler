package frontend.tree;

import frontend.Unit;

import java.util.List;

public class LOrExp extends ExpNode {
    public LOrExp(List<Unit> units) {
        super("LOrExp", units);
    }
}
