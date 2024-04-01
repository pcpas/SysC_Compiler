package backend.MIPSTable;

public class MSymbol {
    final String name;
    final MTable parent;

    public MSymbol(MTable table, String name) {
        this.name = name;
        this.parent = table;
        table.addSymbol(this);
    }

    public String getName() {
        return name;
    }

}
