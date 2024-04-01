package midend.Value;

import java.util.Objects;

public class ConstVal extends Value {
    private String value;
    public ConstVal(ValueType type, String value) {
        super(value, type, true);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isZero() {
        int value = Integer.parseInt(this.value);
        return value == 0;
    }

    public ConstVal i1Totype(ValueType des) {
        assert this.getType().isType(ValueType.IRType.INT1, 0) : "i1Toi32: source const is not a INT1";
        if (value.equals("true")) {
            value = "1";
        } else if (value.equals("false")) {
            value = "0";
        }
        return new ConstVal(des, value);
    }

    public ConstVal Type2I1() {
        if (this.getType().isType(ValueType.IRType.INT32, 0)) {
            if (Integer.parseInt(value) == 0) return new ConstVal(new ValueType(ValueType.IRType.INT1, false), "false");
            else return new ConstVal(new ValueType(ValueType.IRType.INT1, false), "true");
        } else {
            System.out.println("ConstVal: only support Type2I1 for INT32");
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstVal constVal = (ConstVal) o;
        return Objects.equals(value, constVal.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return getType().toString() + " " + value;
    }
}
