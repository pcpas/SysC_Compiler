package frontend.tree;

import frontend.Unit;

import java.util.List;

public class ForStmt extends BasicNode {
    public ForStmt(List<Unit> units) {
        super("ForStmt", units);
    }
}
