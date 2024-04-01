package midend.Value.Instructions;

import midend.Value.Value;
import midend.Value.ValueType;
import frontend.Tokens;

public class IcmpInst extends BinaryInst {
    public Tokens.TokenKind operation;

    public IcmpInst(Value lVar, Value rVar, String resName, Tokens.TokenKind op) {
        super(lVar, rVar, BinaryInstType.ICMP);
        resType = new ValueType(ValueType.IRType.INT1, false);
        setResult(resName);
        operation = op;
    }

    @Override
    public String toString() {
        String res = result.getName();
        String ret = res + " = " + "icmp";
        String op = "";
        switch (operation) {
            case GRE -> op = " sgt ";
            case LSS -> op = " slt ";
            case GEQ -> op = " sge ";
            case LEQ -> op = " sle ";
            case EQL -> op = " eq ";
            case NEQ -> op = " ne ";
        }
        ret += op + lVar.getType() + " ";
        String lVal = lVar.getName();
        String rVal = rVar.getName();
        ret += lVal + ", " + rVal;
        return ret;
    }
}
