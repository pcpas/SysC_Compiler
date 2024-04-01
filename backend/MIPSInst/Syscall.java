package backend.MIPSInst;

import backend.MIPSValues.MBasicBlock;

public class Syscall extends MInstruction {
    public Syscall(MBasicBlock block) {
        super(block);
    }

    @Override
    public String toString() {
        return "syscall";
    }
}
