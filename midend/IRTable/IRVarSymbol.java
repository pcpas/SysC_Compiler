package midend.IRTable;

import midend.Value.ConstVal;
import midend.Value.Value;
import midend.Value.ValueType;

import java.util.List;

public class IRVarSymbol extends IRSymobol{
    public IRVarSymbol(String name, ValueType type, Value value, boolean isConst) {
        super(name, type);
        this.value = value;
        this.isConst = isConst;
    }

    private Value value;
    private boolean isConst;

    //const 数组才有
    public List<ConstVal> consts;

    public boolean isConst(){
        return isConst;
    }

    public Value getValue(){
        return value;
    }

    @Override
    public String toString() {
        return name + " " + (value == null?"val": value.toString());
    }
}
