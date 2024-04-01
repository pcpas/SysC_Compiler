package frontend.tree;

import frontend.Unit;

import java.util.List;

public class LAndExp extends ExpNode {
    public LAndExp(List<Unit> units) {
        super("LAndExp", units);
    }
}
