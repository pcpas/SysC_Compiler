package midend.Value.Instructions;

import midend.Value.Value;
import midend.Value.ValueType;

public class SubInst extends BinaryInst {

    public SubInst(Value lVar, Value rVar, String resName) {
        super(lVar, rVar, BinaryInstType.SUB);
        resType = lVar.getType();
        setResult(resName);
    }

    @Override
    public String toString() {
        ValueType type = resType;
        String ret = getResult().getName() + " = " + "sub" + " " + type.toString() + " ";
        String lVal = lVar.getName();
        String rVal = rVar.getName();
        ret += lVal + ", " + rVal;
        return ret;
    }
}
