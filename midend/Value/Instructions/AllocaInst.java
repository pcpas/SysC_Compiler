package midend.Value.Instructions;

import midend.Value.ValueType;

public class AllocaInst extends Instruction {
    public AllocaInst(ValueType type, String resName) {
        resType = type;
        setResult(resName);
    }

    @Override
    public String toString() {
        String res = result.getName();
        return res + " = " + "alloca" + " " + resType.dePointer().toString();
    }
}
