package backend.MIPSInst;

import backend.MIPSValues.Label;
import backend.MIPSValues.MBasicBlock;
import backend.MIPSValues.MValue;

public class JInst extends MInstruction {

    private final Operation type;
    private final MValue des;


    public boolean isJ(){
        if(type==Operation.J)
            return true;
        else return false;
    }

    public JInst(MBasicBlock block, Operation type, MValue des) {
        super(block);
        this.type = type;
        this.des = des;
    }

    @Override
    public String toString() {
        String ret = null;
        switch (type) {
            case JAL, J, JR -> ret = type + " " + des;
        }
        return ret;
    }

    public boolean isJ2Block(MBasicBlock b){
        return des instanceof Label l && l.isSame(b);
    }

    public enum Operation {
        JAL("jal"),
        J("j"),
        JR("jr");
        final String name;

        Operation(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
