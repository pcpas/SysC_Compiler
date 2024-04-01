package frontend.tree;

import frontend.Unit;

import java.util.List;

public class UnaryOp extends BasicNode {
    public UnaryOp(List<Unit> units) {
        super("UnaryOp", units);
    }
}
