package midend.Value.Instructions;

import midend.Value.Function;
import midend.Value.Value;

import java.util.List;

public class CallInst extends Instruction {

    public Function function;
    public List<Value> params = null;

    public CallInst(Function func, List<Value> params) {
        resType = func.getType();
        createUse(func);
        if(params!=null)
            for(Value param : params){
                createUse(param);
            }
        this.params = params;
        this.function = func;
    }

    public CallInst(Function func, List<Value> params, String resName) {
        this(func, params);
        setResult(resName);
    }

    public int getParamsSize() {
        return params.size();
    }

    @Override
    public String toString() {
        if (result == null)
            return "call " + function.call(params);
        else
            return result.getName() + " = " + "call " + function.call(params);
    }
}
