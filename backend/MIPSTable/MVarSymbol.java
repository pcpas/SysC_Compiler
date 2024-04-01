package backend.MIPSTable;

import backend.MIPSValues.MValue;
import backend.MIPSValues.MemAddr;
import backend.MIPSValues.Register;

public class MVarSymbol extends MSymbol {

    private final VarType type;

    private MValue value;


    public MVarSymbol(MTable table, String name, MValue reg) {
        super(table, name);
        if (reg instanceof Register)
            this.type = VarType.REG;
        else
            this.type = VarType.MEM;
        this.value = reg;
    }

    public VarType getType() {
        return type;
    }

    public MValue getValue() {
        return value;
    }

    public void setValue(MValue value){
        this.value = value;
    }

    @Override
    public String toString() {
        String ret = "Var " + type + " ";
        switch (type) {
            case REG -> ret += name + " " + value;
            case MEM -> ret += name + " " + ((MemAddr) value).getOffset() + "(" + ((MemAddr) value).getBase() + ")";
        }
        return ret;
    }

    public enum VarType {
        REG("reg"), MEM("mem");
        final String name;

        VarType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
