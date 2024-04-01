package exception.symbolTable;

import frontend.tree.Block;
import frontend.tree.FuncFParam;

import java.util.List;

public class FuncSymbol extends Symbol {

    public final Type retType;

    public final List<FuncFParam> params;
    public final Block block;

    //For exception detection
    public FuncSymbol(String name, Type retType, List<FuncFParam> params, Block block) {
        super(name);
        this.retType = retType;
        this.params = params;
        this.block = block;
    }

    @Override
    public String toString() {
        return name + " function";
    }

}
