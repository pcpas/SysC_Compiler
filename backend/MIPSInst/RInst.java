package backend.MIPSInst;

import backend.MIPSValues.MBasicBlock;
import backend.MIPSValues.MValue;
import backend.MIPSValues.Register;

import java.util.HashSet;
import java.util.Set;

public class RInst extends MInstruction {

    public final RInst.Operation operation;
    private final MValue rd;
    public final MValue rt;

    public RInst(MBasicBlock block, Operation type, MValue rd, MValue rs, MValue rt) {
        super(block);
        this.operation = type;
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }

    @Override
    public String toString() {
        String ret;
        switch (operation) {
            case MFLO,MFHI -> ret = operation + " " + rd;
            default -> ret = operation + " " + rd + ", " + rs + ", " + rt;
        }
        return ret;
    }


    public boolean isDead(){
        if(operation == Operation.ADDU){
            return (rd == rs || rd == rt) && (rs == Register.R0 || rt == Register.R0);
        }else if(operation == Operation.SUBU){
            return rd == rs && rt==Register.R0;
        }
        return false;
    }

    @Override
    public MValue getModified(){
        MValue res = null;
        res = rd;
        return res;
    }

    @Override
    public Set<MValue> getUsed(){
        Set<MValue> res =  new HashSet<>();
        switch (operation){
            case MFLO,MFHI -> {

            }
            default -> {
                res.add(rt);
                res.add(rs);
            }
        }
        return res;
    }



    public enum Operation {
        SLT("slt"),
        SLTU("sltu"),
        SUBU("subu"),
        ADDU("addu"),
        MUL("mul"),
        REM("rem"),
        DIV("div"),
        MFLO("mflo"),
        MFHI("mfhi"),
        XOR("xor"),
        BEQ("beq");
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
