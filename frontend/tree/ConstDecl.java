package frontend.tree;

import frontend.Unit;

import java.util.List;

public class ConstDecl extends BasicNode {
    public ConstDecl(List<Unit> units) {
        super("ConstDecl", units);
    }

}
