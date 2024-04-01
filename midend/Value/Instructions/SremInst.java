package midend.Value.Instructions;

import midend.Value.Value;
import midend.Value.ValueType;

public class SremInst extends BinaryInst {
    public SremInst(Value lVar, Value rVar, String resName) {
        super(lVar, rVar, BinaryInstType.SREM);
        resType = lVar.getType();
        setResult(resName);
    }

    @Override
    public String toString() {
        ValueType type = resType;
        String ret = getResult().getName() + " = " + "srem" + " " + type.toString() + " ";
        String lVal = lVar.getName();
        String rVal = rVar.getName();
        ret += lVal + ", " + rVal;
        return ret;
    }
}
