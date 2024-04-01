package backend.MIPSValues;

import midend.Value.Value;

import java.util.*;

public class MFunction implements MValue {
    private final List<MBasicBlock> blockList;
    private final String name;
    private final FrameStack frameStack;

    public boolean isLeaf = false;

    public List<Value> params = new ArrayList<>();

    private final Map<Value, Register> globalValueRegMap = new HashMap<>();

    public MFunction(MModule parent, String name, FrameStack frameStack) {
        this.frameStack = frameStack;
        parent.addFunction(this);
        this.blockList = new ArrayList<>();
        this.name = name;
    }

    protected void addBasicBlock(MBasicBlock block) {
        blockList.add(block);
    }

    public List<MBasicBlock> getBLockList() {
        return blockList;
    }

    public String getName() {
        return name;
    }

    public void addParam(Value value){
        params.add(value);
    }

    public List<Value> getParams(){
        return params;
    }
    public FrameStack getFrameStack() {
        return frameStack;
    }

    //使用引用计数获得全局寄存器分配
    public void getGlobalRegMap1(Queue<Value> counter, Set<Register> globals){
        globalValueRegMap.clear();
        if(counter!=null)
        {
            for (Register reg : globals){
                globalValueRegMap.put(counter.poll(), reg);
            }
        }
    }

    public Map<Value, Register> getGlobalValueRegMap() {
        return globalValueRegMap;
    }

    public void setGlobalValueRegMap2(Map<Value, Register> map) {
        globalValueRegMap.clear();
        globalValueRegMap.putAll(map);
    }

    public Register getGlobalReg(Value value){
        return globalValueRegMap.get(value);
    }
}
