package midend.Value.Instructions;

import midend.Value.Value;

public class MoveInst extends Instruction{
    public Value source;
    public MoveInst(Value source, Value des){
        setResult(des);
        this.source = source;
        createUse(source);
    }

    @Override
    public String toString() {
        return "zzq_move " + getResult().toString() + ", " + source.toString();
    }
}
