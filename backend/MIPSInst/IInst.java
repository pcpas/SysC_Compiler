package backend.MIPSInst;

import backend.MIPSValues.*;

import java.util.HashSet;
import java.util.Set;


public class IInst extends MInstruction {
    public final Operation operation;
    public final MValue rt;
    public Immediate immediate;

    public IInst(MBasicBlock block, Operation operation, MValue rs, MValue rt, Immediate immediate) {
        super(block);
        this.operation = operation;
        this.rs = rs;
        this.rt = rt;
        this.immediate = immediate;
    }

    @Override
    public String toString() {
        String ret;
        switch (operation) {
            case SW, LW -> {
                if (immediate == null)
                    ret = operation + " " + rt + ", 0(" + rs + ")";
                else {
                    ret = operation + " " + rt + ", " + immediate + "(" + rs + ")";
                }

            }
            case MOVE,MULT -> ret = operation + " " + rs + ", " + rt;
            case LI -> ret = operation + " " + rs + " " + immediate;
            case BNEZ, LA -> ret = operation + " " + rs + " " + rt;
            default -> ret = operation + " " + rs + ", " + rt + ", " + immediate;
        }
        return ret;
    }

    @Override
    public MValue getModified(){
        MValue res = null;
        switch (operation){
            case LW -> res = rt;
            case BNEZ -> res = null;
            default -> res = rs;
        }
        return res;
    }

    @Override
    public Set<MValue> getUsed(){
        Set<MValue> res =  new HashSet<>();
        switch (operation){
            case LW, BNEZ -> {
                res.add(rs);
            }
            case SW -> {
                res.add(rt);
                res.add(rs);
            }
            case LA,LI -> {

            }
            default -> {
                res.add(rt);
            }
        }
        return res;
    }

    public enum Operation {
        ADDIU("addiu"),
        SW("sw"),
        MOVE("move"),
        LI("li"),
        LW("lw"),
        BNEZ("bnez"),
        XORI("xori"),
        SLTIU("sltiu"),
        LA("la"),
        SLL("sll"),
        SRA("sra"),
        SRL("srl"),
        MULT("mult");

        final String name;

        Operation(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public boolean isPutchLI1() {
        if(operation == Operation.LI && rs == Register.R2 && ((ImmediateNumber)immediate).getValue().equals("11"))
            return true;
        return false;
    }

    public boolean isPutchLI2() {
        if(operation == Operation.LI && rs == Register.R4)
            return true;
        return false;
    }

    public boolean isDead(){
        if(operation == Operation.ADDIU){
            if(rt == rs && immediate instanceof ImmediateNumber i){
                if(i.getValue().equals("0"))
                    return true;
            }
        }
        return false;
    }

    public Immediate getImmediate() {
        return immediate;
    }
}
