package midend.IRTable;

import midend.Value.Function;
import midend.Value.ValueType;

public class IRFuncSymbol extends IRSymobol {
    public IRFuncSymbol(String name, ValueType type, Function function) {
        super(name, type);
        this.function = function;
    }

    private Function function;

    public Function getFunction() {
        return function;
    }
}
