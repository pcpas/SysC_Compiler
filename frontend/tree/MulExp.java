package frontend.tree;

import frontend.Unit;

import java.util.List;

public class MulExp extends ExpNode {
    public MulExp(List<Unit> units) {
        super("MulExp", units);
    }
}
