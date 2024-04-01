package backend.MIPSValues;

import java.util.List;

public class MGlobalVar implements MValue {
    private final InitType initType;
    private final String name;
    private final List<String> initVals;

    public MGlobalVar(MModule parent, InitType initType, String name, List<String> initVals) {
        parent.addGlobalVar(this);
        this.initType = initType;
        this.name = name;
        this.initVals = initVals;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder(name + ": " + initType + " ");
        for (int i = 0; i < initVals.size(); i++) {
            ret.append(initVals.get(i));
            if (i != initVals.size() - 1)
                ret.append(", ");
        }
        return ret.toString();
    }

    public enum InitType {
        WORD(".word"),
        SPACE(".space"),
        ASCII(".asciiz");
        final String name;

        InitType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
