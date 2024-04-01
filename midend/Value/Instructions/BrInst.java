package midend.Value.Instructions;

import midend.Value.BasicBlock;
import midend.Value.Value;
import midend.Value.ValueType;

public class BrInst extends Instruction {
    public BasicBlock b1 = null;
    public BasicBlock b2 = null;
    public Value cond = null;

    public BrInst(Value Cond, BasicBlock b1, BasicBlock b2) {
        assert Cond != null : "BrInst can't take a null Cond";
        assert Cond.getType().isType(ValueType.IRType.INT1, 0) : "br cond is not a i1 var " + Cond;
        cond = Cond;
        this.b1 = b1;
        this.b2 = b2;
        createUse(cond);
        createUse(b1);
        createUse(b2);
    }

    public BrInst(BasicBlock b) {
        this.b1 = b;
        createUse(b1);
    }

    public boolean isCond() {
        return cond != null;
    }

    @Override
    public String toString() {
        if (cond != null) {
            assert b1 != null && b2 != null : "BrInst: b1 or b2 has not assigned";
            return "br " + cond + ", label %" + b1.getLabel() + ", label %" + b2.getLabel();
        } else {
            assert b1 != null : "BrInst: b1 has not assigned";
            return "br " + " label %" + b1.getLabel();
        }
    }
}
