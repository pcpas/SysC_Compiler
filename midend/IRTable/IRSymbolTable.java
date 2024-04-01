package midend.IRTable;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRSymbolTable {
    private final Map<String, IRSymobol> symbolMap = new HashMap<>();
    private final IRSymbolTable parent;
    private final List<IRSymbolTable> sons = new ArrayList<>();

    public IRSymbolTable(IRSymbolTable parent){
        this.parent = parent;
        if(parent!=null)
            parent.addSon(this);
    }

    private void addSon(IRSymbolTable son){
        sons.add(son);
    }

    public void AddSymbol(IRSymobol symobol){
        symbolMap.put(symobol.getName(), symobol);
    }

    public IRSymobol findSymbolInThis(String name) {
        return symbolMap.get(name);
    }

    public IRSymobol findSymbolInAll(String name) {
        IRSymbolTable temp = this;
        IRSymobol symbol = null;
        while (temp != null && symbol == null) {
            symbol = temp.findSymbolInThis(name);
            temp = temp.parent;
        }
        if (symbol == null) {
            System.out.println("MTable: cannot find symbol " + name);
        }
        return symbol;
    }

    public void debug(int depth) {
        // 输出当前表的信息
        System.out.println("=".repeat(4 + depth * 4) + " IRTable Info " + "=".repeat(4 + depth * 4));
        for (IRSymobol symbol : symbolMap.values()) {
            System.out.println("  ".repeat(depth) + symbol);
        }

        // 递归打印子表的信息
        for (IRSymbolTable son : sons) {
            son.debug(depth + 1);
        }
        System.out.println("=".repeat(4 + depth * 4) + " IRTable End  " + "=".repeat(4 + depth * 4));
    }

    public IRSymbolTable getParent() {
        return parent;
    }
}

