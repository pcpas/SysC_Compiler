package midend.Value.Instructions;

import midend.Value.Value;

public class StoreInst extends Instruction {
    public Value lVar;
    public Value rVar;

    public StoreInst(Value lVar, Value rVar, Value real) {
        assert lVar.getType().getPointer() == rVar.getType() : "StoreInst: types don't match";
        createUse(lVar);
        //if(rVar!=real)
            createUse(rVar);
        setResult(real);
        this.lVar = lVar;
        this.rVar = rVar;
    }

    @Override
    public String toString() {
        return "store " + lVar.toString() + ", " + rVar.toString();
    }
}
