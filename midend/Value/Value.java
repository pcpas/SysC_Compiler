package midend.Value;

import java.util.ArrayList;
import java.util.List;

public class Value extends IRValue{
    private final String name;
    private final ValueType type;
    private boolean isConst = false;
    protected List<ConstVal> values;

    //only var-values have name&type.
    public Value(String name, ValueType type, boolean isConst) {
        this.name = name;
        this.type = type;
        this.isConst = isConst;
        this.values = new ArrayList<>();
    }

    public boolean isConst() {
        return isConst;
    }

    public String getName() {
        return name;
    }

    public ValueType getType() {
        return type;
    }

    public List<ConstVal> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return type.toString() + " " + name;
    }
}
