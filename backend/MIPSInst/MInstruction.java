package backend.MIPSInst;

import backend.MIPSValues.MBasicBlock;
import backend.MIPSValues.MValue;

import java.util.HashSet;
import java.util.Set;

public class MInstruction implements MValue {
    MBasicBlock parent;
    public MValue rs;

    public MInstruction(MBasicBlock block) {
        parent = block;
        block.addInst(this);
    }

    public MValue getModified() {
        return null;
    }

    public Set<MValue> getUsed(){return new HashSet<>();}
}
