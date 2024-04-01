package midend.Value.Instructions;

import midend.Value.Value;
import midend.Value.ValueType;

public class AddInst extends BinaryInst {

    public AddInst(Value lVar, Value rVar, String resName) {
        super(lVar, rVar, BinaryInstType.ADD);
        resType = lVar.getType();
        setResult(resName);
    }

    @Override
    public String toString() {
        ValueType type = resType;
        String res = result.getName();
        String ret = res + " = " + "add" + " " + type.toString() + " ";
        String lVal = lVar.getName();
        String rVal = rVar.getName();
        ret += lVal + ", " + rVal;
        return ret;
    }
}
