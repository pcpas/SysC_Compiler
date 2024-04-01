package midend.Value.Instructions;

import midend.Value.Value;
import midend.Value.ValueType;

public class ZextInst extends Instruction {
    public final Value source;

    public ZextInst(Value value, ValueType des, String resName) {
        resType = des;
        createUse(value);
        source = value;
        setResult(resName);
    }

    @Override
    public String toString() {
        String res = result.getName();
        return res + " = " + "zext " + source + " to " + resType;
    }
}
