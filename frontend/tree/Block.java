package frontend.tree;

import frontend.Unit;

import java.util.List;

public class Block extends BasicNode {
    public Block(List<Unit> units) {
        super("Block", units);
    }
}
