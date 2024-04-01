package midend.Value.Instructions;

import midend.Value.Value;

public class BinaryInst extends Instruction {

    public final Value lVar;
    public final Value rVar;
    public final BinaryInstType type;

    public BinaryInst(Value lVar, Value rVar, BinaryInstType type) {
        assert lVar.getType() == rVar.getType();
        createUse(lVar);
        createUse(rVar);
        this.lVar = lVar;
        this.rVar = rVar;
        this.type = type;
    }

    public enum BinaryInstType {
        ADD, ICMP, MUL, SDIV, SREM, SUB;
    }
}
