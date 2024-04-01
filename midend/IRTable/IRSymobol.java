package midend.IRTable;

import midend.Value.ValueType;

public class IRSymobol {
     final String name;
     final ValueType type;


    public IRSymobol(String name, ValueType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public ValueType getType() {
        return type;
    }
}
