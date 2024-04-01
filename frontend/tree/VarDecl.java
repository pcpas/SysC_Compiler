package frontend.tree;

import frontend.Unit;

import java.util.List;

public class VarDecl extends BasicNode {
    public VarDecl(List<Unit> units) {
        super("VarDecl", units);
    }
}
