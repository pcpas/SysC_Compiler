package backend.MIPSTable;

public class MFunctionSymbol extends MSymbol {
    private final MTable funcTable;
    private final int paramSize;

    public MFunctionSymbol(MTable table, String name, int paramSize, MTable funcTable) {
        super(table, name);
        this.paramSize = paramSize;
        this.funcTable = funcTable;
    }

    public int getParamSize() {
        return paramSize;
    }

    @Override
    public String toString() {
        return "Function " + name;
    }
}
