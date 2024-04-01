package frontend.tree;

import frontend.Unit;

import java.util.List;

public class ConstExp extends ExpNode {
    public ConstExp(List<Unit> units) {
        super("ConstExp", units);
    }
}
