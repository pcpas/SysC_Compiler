package midend.Value;

import java.util.List;

public class GlobalVar extends Value {


    public GlobalVar(String name, ValueType type, boolean isConst, ConstVal initVal) {
        super(name, type.getPointer(), isConst);
        values.add(initVal);
    }

    public GlobalVar(String name, ValueType type, List<ConstVal> initVal, boolean isConst) {
        super(name, type.getPointer(), isConst);
        values = initVal;
    }

    public String toDecl() {
        StringBuilder ret = new StringBuilder(this.getName() + " = " + "dso_local " + (this.isConst() ? "constant " : "global ") + this.getType().dePointer() + " ");
        if (this.getType().isArray()) {
            if (values == null) {
                ret.append("zeroinitializer");
            } else {
                assert this.getType().getDimension() <= 2 : "GlobalVarDecl: only support 1D or 2D array~";
                List<Integer> lens = this.getType().getLens();
                if (this.getType().getDimension() == 1) {
                    ret.append("[");
                    int cnt = 0;
                    for (ConstVal val : values) {
                        ret.append(val);
                        if (++cnt < values.size())
                            ret.append(", ");
                        else ret.append("]");
                    }
                } else if (this.getType().getDimension() == 2) {
                    ret.append("[");
                    ValueType t = this.getType().deArray().dePointer();
                    for (int i = 0; i < lens.get(0); i++) {
                        ret.append(t).append(" ");
                        if (isRowAllZeros(values, i, lens.get(1))) {
                            ret.append("zeroinitializer");
                        } else {
                            ret.append("[");
                            int cnt = 0;
                            for (int j = 0; j < lens.get(1); j++) {
                                ret.append(values.get(i * lens.get(1) + j));
                                if (++cnt < lens.get(1)) {
                                    ret.append(", ");
                                } else {
                                    ret.append("]");
                                }
                            }
                        }
                        if (i < lens.get(0) - 1) {
                            ret.append(", ");
                        }
                    }
                    ret.append("]");
                }
            }
        } else {
            ret.append(values.get(0).getValue());
        }
        return ret.toString();
    }

    private boolean isRowAllZeros(List<ConstVal> initVal, int row, int rowLength) {
        for (int j = 0; j < rowLength; j++) {
            if (!initVal.get(row * rowLength + j).isZero()) {
                return false;
            }
        }
        return true;
    }
}
