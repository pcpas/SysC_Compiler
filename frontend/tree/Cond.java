package frontend.tree;

import frontend.Unit;

import java.util.List;

public class Cond extends BasicNode {
    public Cond(List<Unit> units) {
        super("Cond", units);
    }
}
