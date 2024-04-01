package midend.Value;

import frontend.Tokens;
import exception.symbolTable.Type;

import java.util.List;

public class ValueType {
    private final int dimension;
    private final IRType type;
    private final boolean isPointer;
    private final List<Integer> lens;
    private int pointerCnt = 0;

    public ValueType(IRType type, boolean isPointer) {
        dimension = 0;
        this.type = type;
        lens = null;
        this.isPointer = isPointer;
        if (isPointer) pointerCnt = 1;
    }

    public ValueType(IRType type, boolean isPointer, int dimension, List<Integer> lens) {
        this.dimension = dimension;
        this.type = type;
        this.lens = lens;
        this.isPointer = isPointer;
        if (isPointer) pointerCnt = 1;
    }

    private ValueType(IRType type, boolean isPointer, int dimension, List<Integer> lens, int pointerCnt) {
        this.dimension = dimension;
        this.type = type;
        this.lens = lens;
        this.isPointer = isPointer;
        this.pointerCnt = pointerCnt;
    }

    public static ValueType toValueType(Type type) {
        ValueType ret = null;
        switch (type) {
            case INT -> ret = new ValueType(IRType.INT32, false);
            case VOID -> ret = new ValueType(IRType.VOID, false);
        }
        if (ret == null) {
            System.out.println("Type 2 ValueType failed");
        }
        return ret;
    }

    public static ValueType toValueType(Tokens.Token type) {
        return toValueType(Type.toType(type));
    }

    public ValueType getPointer() {

        return new ValueType(type, true, dimension, lens, pointerCnt + 1);
    }

    public ValueType getArrayPointer() {

        return new ValueType(type, true, dimension, lens, pointerCnt + 1);
    }

    public ValueType getArray(int dimension, List<Integer> lens) {
        return new ValueType(type, isPointer, dimension, lens, pointerCnt);
    }

    public ValueType dePointer() {
        if (pointerCnt == 1)
            return new ValueType(type, false, dimension, lens, pointerCnt - 1);
        else
            return new ValueType(type, true, dimension, lens, pointerCnt - 1);
    }

    public ValueType deArray() {
        assert dimension > 0 : "ValueType: you can't deArray a non-array";
        if (dimension == 1) {
            return new ValueType(type, isPointer, 0, null, pointerCnt);
        } else
            return new ValueType(type, isPointer, dimension - 1, lens.subList(1, dimension), pointerCnt);
    }

    public ValueType getBtype() {
        return new ValueType(type, false);
    }

    @Override
    public String toString() {
        StringBuilder ret;
        if (dimension == 0) {
            ret = new StringBuilder(type.toString());
        } else {
            assert lens != null;
            ret = new StringBuilder(genArrayType(type, lens));
        }
        ret.append("*".repeat(Math.max(0, pointerCnt)));
        return ret.toString();
    }

    private String genArrayType(ValueType.IRType unitType, List<Integer> d) {
        if (d.isEmpty()) {
            return unitType.tag;
        }
        int dimensions = d.size();
        if (dimensions == 1) {
            return "[" + d.get(0) + " x " + unitType + "]";
        } else {
            StringBuilder result = new StringBuilder("[");
            for (int i = 0; i < dimensions - 1; i++) {
                result.append(d.get(i)).append(" x ");
            }
            result.append(genArrayType(unitType, d.subList(dimensions - 1, dimensions)));
            result.append("]");
            return result.toString();
        }
    }

    public boolean isType(IRType desType, int desDimension) {
        return type == desType && dimension == desDimension;
    }

    public boolean isPointer() {
        return isPointer;
    }

    public boolean isArray() {
        return dimension > 0;
    }

    public int getDimension() {
        return dimension;
    }

    public int getTypeByte() {
        if (type == IRType.INT32 && pointerCnt == 0 && dimension == 0)
            return 4;
        else {
            System.out.println("getTypeByte: 错误的使用方法！");
            return 0;
        }
    }

    public List<Integer> getLens() {
        return lens;
    }

    public enum IRType {
        VOID("void"),
        INT1("i1"),
        INT32("i32");

        final String tag;

        IRType(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return tag;
        }
    }

}
