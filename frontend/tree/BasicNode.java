package frontend.tree;

import frontend.Unit;

import java.util.ArrayList;
import java.util.List;

public class BasicNode implements Unit {
    public final String name;
    public final List<Unit> derivations;

    public BasicNode(String name, List<Unit> units) {
        this.name = name;
        this.derivations = new ArrayList<>(units);
    }

    @Override
    public String toString() {
        return derivations.stream()
                .map(Unit::toString)
                .reduce((s1, s2) -> s1 + "\n" + s2).orElse("") + "\n<" + name + ">";
    }

    // 递归打印方法
    public void debug() {
        printTree(this, 0);
    }

    private void printTree(Unit unit, int depth) {
        if (unit instanceof BasicNode) {
            System.out.println("  ".repeat(Math.max(0, depth)) // 两个空格作为缩进
                    + ((BasicNode) unit).name);
        } else {
            System.out.println("  ".repeat(Math.max(0, depth)) // 两个空格作为缩进
                    + unit);
        }

        if (unit instanceof BasicNode basicNode) {
            for (Unit derivation : basicNode.derivations) {
                printTree(derivation, depth + 1);
            }
        }
    }

}