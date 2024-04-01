package backend.MIPSTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MTable {
    private final MTable parent;
    private final Map<String, MSymbol> symbolMap;
    private final List<MTable> sons;

    public MTable(MTable parent) {
        this.parent = parent;
        symbolMap = new HashMap<>();
        sons = new ArrayList<>();
        if (parent != null)
            parent.sons.add(this);
    }

    public MSymbol findSymbolInThis(String name) {
        return symbolMap.get(name);
    }

    public MSymbol findSymbolInAll(String name) {
        MTable temp = this;
        MSymbol symbol = null;
        while (temp != null && symbol == null) {
            symbol = temp.findSymbolInThis(name);
            temp = temp.parent;
        }
        if (symbol == null) {
            System.out.println("MTable: cannot find symbol " + name);
        }
        return symbol;
    }

    public void addSymbol(MSymbol symbol) {
        symbolMap.put(symbol.getName(), symbol);
    }

    public MTable getParent() {
        return parent;
    }

    public void debug(int depth) {
        // 输出当前表的信息
        System.out.println("=".repeat(4 + depth * 4) + " MTable Info " + "=".repeat(4 + depth * 4));
        for (MSymbol symbol : symbolMap.values()) {
            System.out.println("  ".repeat(depth) + symbol);
        }

        // 递归打印子表的信息
        for (MTable son : sons) {
            son.debug(depth + 1);
        }
        System.out.println("=".repeat(4 + depth * 4) + " MTable End  " + "=".repeat(4 + depth * 4));
    }
}
