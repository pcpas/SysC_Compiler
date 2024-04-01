package backend.MIPSValues;

import java.util.Arrays;

public enum Register implements MValue {
    R0("zero"), R1("at"),
    R2("v0"), R3("v1"),
    R4("a0"), R5("a1"), R6("a2"), R7("a3"),
    R8("t0"), R9("t1"), R10("t2"), R11("t3"),
    R12("t4"), R13("t5"), R14("t6"), R15("t7"),
    R16("s0"), R17("s1"), R18("s2"), R19("s3"),
    R20("s4"), R21("s5"), R22("s6"), R23("s7"),
    R24("t8"), R25("t9"),
    R26("k0"), R27("k1"),
    R28("gp"), R29("sp"), R30("fp"), R31("ra");

    private final String name;

    Register(String name) {
        this.name = "$" + name;
    }

    public String getName() {
        return name;
    }

    public boolean isTemp() {
        return name.matches("\\$t[0-9]");
    }

    public boolean isSaved() {
        return name.matches("\\$s[0-7]");
    }

    public boolean isMySaved(){
        return name.matches("\\$a[1-3]")
                || name.matches("\\$s[6-7]")
                || name.matches("\\$k[0-1]")
                || name.matches("\\$t[3-8]")
                || name.matches("\\$v1")
                || name.matches("\\$gp");
    }

    public boolean isMyTemp(){
        return name.matches("\\$t[0-2]");
    }

    public boolean isMyGlobal(){
        return name.matches("\\$s[0-5]");
//        return false;
    }



    public boolean isMyMagic() {
        return isMyGlobal() || name.equals("$fp") || name.equals("$sp") || name.equals("$a0") || name.equals("$v0");
    }

    public boolean isParam() {
        return name.matches("\\$a[0-3]");
    }

    @Override
    public String toString() {
        return name;
    }
}
