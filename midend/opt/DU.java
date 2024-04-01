package midend.opt;

import midend.Value.Instructions.Instruction;

public class DU {
    Instruction inst;
    int type;

    public Instruction getInst() {
        return inst;
    }

    public boolean isDef(){
        return type==1;
    }

    public DU(Instruction inst, int type){
        this.inst = inst;
        this.type = type;
    }
}
