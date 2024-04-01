package midend.Value.Instructions;

import midend.Value.ConstVal;
import midend.Value.Value;
import midend.Value.ValueType;

import java.util.List;

public class GetElementPtrInst extends Instruction {
    public final ValueType basicType;
    public final List<Value> indexs;
    public final Value source;

    public GetElementPtrInst(ValueType basicType, Value source, List<Value> indexs, String name) {
        this.basicType = basicType;
        this.indexs = indexs;
        this.source = source;
        createUse(source);
        for (Value value : indexs) {
            if (!(value instanceof ConstVal))
                createUse(value);
        }
        resType = basicType;
        //System.out.println(indexs);
        for (int i = 1; i < indexs.size(); i++) {
            resType = resType.deArray();
        }
        resType = resType.getPointer();
        setResult(name);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(result.getName()).append(" = getelementptr ").append(basicType).append(", ")
                .append(source).append(", ");
        int cnt = 0;
        for (Value value : indexs) {
            ret.append(value);
            if (++cnt < indexs.size()) {
                ret.append(", ");
            }
        }
        return ret.toString();
    }
}
