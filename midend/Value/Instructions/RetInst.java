package midend.Value.Instructions;

import midend.Value.Value;
import midend.Value.ValueType;

public class RetInst extends Instruction {

    public final ValueType retType;
    public Value retVal;

    public RetInst() {
        retType = new ValueType(ValueType.IRType.VOID, false);
    }

    public RetInst(Value retVal) {
        createUse(retVal);
        this.retVal = retVal;
        retType = retVal.getType();
    }

    @Override
    public String toString() {
        String ret = "ret" + " ";
        if (!retType.isType(ValueType.IRType.VOID, 0))
            ret += retVal;
        else {
            ret += retType;
        }
        return ret;
    }

}
