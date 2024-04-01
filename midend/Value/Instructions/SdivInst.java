package midend.Value.Instructions;

import midend.Value.Value;
import midend.Value.ValueType;

public class SdivInst extends BinaryInst {
    public SdivInst(Value lVar, Value rVar, String resName) {
        super(lVar, rVar, BinaryInstType.SDIV);
        resType = lVar.getType();
        setResult(resName);
    }

    @Override
    public String toString() {
        ValueType type = resType;
        String ret = getResult().getName() + " = " + "sdiv" + " " + type.toString() + " ";
        String lVal = lVar.getName();
        String rVal = rVar.getName();
        ret += lVal + ", " + rVal;
        return ret;
    }
}
