package frontend.tree;

import frontend.Unit;
import exception.symbolTable.SymbolTable;

import java.util.List;

public class ExpNode extends BasicNode {

    Boolean isConst = null;
    int dimension = -1;

    public ExpNode(String name, List<Unit> units) {
        super(name, units);
    }


    //这里的dimension不是指这个符号的维数，而是指
    public int getDimension(SymbolTable symbolTable) {
        if (dimension != -1) return dimension;
        for (Unit unit : derivations) {
            if (unit instanceof ExpNode) {
                dimension = ((ExpNode) unit).getDimension(symbolTable);
                break;//因为不存在非法操作，因此测第一个就行，需要再改！
            }
        }
        return dimension;
    }

    public boolean isConst() {
        //目前用不到这个函数，需要再改！
        return isConst;
    }
}
