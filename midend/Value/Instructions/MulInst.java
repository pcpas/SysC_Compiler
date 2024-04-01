package midend.Value.Instructions;

import midend.Value.Value;
import midend.Value.ValueType;

public class MulInst extends BinaryInst {

    public MulInst(Value lVar, Value rVar, String resName) {
        super(lVar, rVar, BinaryInstType.MUL);
        resType = lVar.getType();
        setResult(resName);
    }

    @Override
    public String toString() {
        ValueType type = resType;
        String ret = getResult().getName() + " = " + "mul" + " " + type.toString() + " ";
        String lVal = lVar.getName();
        String rVal = rVar.getName();
        ret += lVal + ", " + rVal;
        return ret;
    }
}
