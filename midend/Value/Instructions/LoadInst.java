package midend.Value.Instructions;

import midend.Value.Value;

public class LoadInst extends Instruction {
    public Value source;
    public Value real;
    public LoadInst(Value value, String resName, Value real) {
        createUse(value);
        source = value;
        resType = value.getType().dePointer();
        this.real = real;
        setResult(resName);
    }

    @Override
    public String toString() {
        return result.getName() + " = " + "load " + resType.toString() + ", " + source.toString();
    }
}
