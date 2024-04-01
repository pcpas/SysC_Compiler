package exception.symbolTable;

public class VarSymbol extends Symbol {

    public final int dimension;
    public final boolean isConst;


    public VarSymbol(String name, int dimension, boolean isConst) {
        super(name);
        this.dimension = dimension;
        this.isConst = isConst;
    }


    @Override
    public String toString() {
        return name + " " +"val";
    }
}
