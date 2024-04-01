package exception.symbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Symbol> symbolMap = new HashMap<>();
    private final SymbolTable parent;
    private final boolean inLoop;
    private final List<SymbolTable> sons = new ArrayList<>();

    public SymbolTable(SymbolTable parent, boolean inLoop) {
        this.parent = parent;
        this.inLoop = inLoop;
        if (parent != null)
            parent.sons.add(this);
    }

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
        if (parent != null) {
            parent.sons.add(this);
            this.inLoop = parent.inLoop;
        } else
            this.inLoop = false;
    }

    public Symbol findSymbol(String name) {
        Symbol symbol = symbolMap.get(name);
        return symbol;
    }

    public boolean isInLoop() {
        return inLoop;
    }

    public Symbol findSymbolInAll(String name) {
        SymbolTable temp = this;
        Symbol symbol = null;
        while (temp != null && symbol == null) {
            symbol = temp.findSymbol(name);
            temp = temp.parent;
        }
        if (symbol == null) {
            System.out.println("cannot find symbol " + name);
            log(0);
        }
        return symbol;
    }

    public void addSymbol(Symbol symbol) {
        symbolMap.put(symbol.name, symbol);
    }


    public SymbolTable getParent() {
        return parent;
    }

    public int size() {
        return symbolMap.size();
    }

    public void log(int depth) {
        if (depth == 0)
            System.out.println("------------------Log--------------------");
        String indent = "  ".repeat(depth); // 创建缩进字符串，根据深度进行缩进

        for (Symbol symbol : symbolMap.values()) {
            System.out.println(indent + symbol);
        }

        for (SymbolTable son : sons) {
            System.out.println(indent + "---- Subtable ----");
            son.log(depth + 1); // 递归调用 log 方法，增加深度
        }
    }
}
