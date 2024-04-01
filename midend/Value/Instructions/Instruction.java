package midend.Value.Instructions;

import midend.Value.User;
import midend.Value.Value;
import midend.Value.ValueType;

public class Instruction extends User {
    ValueType resType = null;
    Value result = null;

    public ValueType getResType() {
        return resType;
    }

    public Value getResult() {
        if (result == null) {
            //System.out.println("inst need a result!");
            return null;
        }
        if (result.getType() == null) {
            System.out.println(result.getName() + " need a type");
        }
        return result;
    }

    public void setResult(String name) {
        result = new Value(name, resType, true);
    }

    public void setResult(Value value){
        result = value;
    }
}
