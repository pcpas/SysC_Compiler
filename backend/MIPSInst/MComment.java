package backend.MIPSInst;

import backend.MIPSValues.MBasicBlock;
import midend.Value.Instructions.Instruction;

public class MComment extends MInstruction{
    String comment;

    public MComment(MBasicBlock block, Instruction inst) {
        super(block);
        comment = "\n# "+inst.toString();
    }

    @Override
    public String toString(){
        return comment;
    }
}
