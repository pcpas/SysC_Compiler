package frontend.tree;

import frontend.Tokens;
import frontend.Unit;

import java.util.List;

public class FuncFParam extends BasicNode {
    private int dimension = -1;

    public FuncFParam(List<Unit> units) {
        super("FuncFParam", units);
    }

    public Tokens.Token getIdent() {
        return ((Tokens.Token) derivations.get(1));
    }

    public int getDimension() {
        if (dimension != -1) return dimension;
        if (derivations.size() >= 2)
        {
            dimension = 0;
            for(Unit unit : derivations)
            {
                if(unit instanceof Tokens.Token t && t.getKind()== Tokens.TokenKind.LBRACK)
                    dimension++;
            }
        }
        return dimension;
    }

}
