package midend.Value;

import midend.IRModule;

import java.util.*;

public class Function extends IRValue {
    private final String funcName;
    private final ValueType retType;
    private final List<Value> params = new ArrayList<>();
    private final IRModule parent;
    private List<BasicBlock> blocks = new ArrayList<>();

    private Queue<Value> refCounter = null;

    Map<Value, Set<Value>> conflictGraph = null;

    private int virtualReg = -1;
    private boolean isLeaf = false;

    public boolean isLeaf(){
        return isLeaf;
    }

    public void setLeaf(){
        isLeaf = true;
    }

    public Function(String name, ValueType type, IRModule parent) {
        this.funcName = name;
        this.retType = type;
        this.parent = parent;
        if (parent != null) {
            //System.out.println(retType + " " + funcName + " has been created~");
            parent.addFunction(this);
        }
    }

    public String nextLabel() {
        virtualReg++;
        return String.valueOf(virtualReg);
    }

    public String nextReg() {
        virtualReg++;
        return "%" + virtualReg;
    }

    public List<Value> getParams() {
        return params;
    }

    public List<BasicBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<BasicBlock> list) {
        this.blocks = list;
    }

    public void addParam(Value param) {
        if (param == null) {
            System.out.println("Add a null into params of " + funcName);
        }
        params.add(param);
    }

    public void addBlock(BasicBlock block) {
        blocks.add(block);
    }

    public String toString() {
        return "define dso_local " + retType + " " + funcName + " (" +
                params.stream().map(Value::toString)
                        .reduce((x, y) -> x + ", " + y).orElse("") + ")";
    }

    public ValueType getType() {
        return retType;
    }

    public String call(List<Value> params) {
        StringBuilder paramStr = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            for (Value param : params) {
                if (!paramStr.isEmpty()) {
                    paramStr.append(", ");
                }
                paramStr.append(param.toString());
            }
        }

        return retType + " " + funcName + "(" + paramStr.toString() + ")";
    }

    public void sortBlocksByLabel() {
        blocks.sort((block1, block2) -> {

            int l = extractNumber(block1.getLabel());
            int r = extractNumber(block2.getLabel());
            return l - r;
        });
    }
    private int extractNumber(String label) {
        // 使用正则表达式提取数字部分
        String[] parts = label.split("_");
        if (parts.length > 0) {
            String numericPart = parts[0];
            try {
                return Integer.parseInt(numericPart);
            } catch (NumberFormatException e) {
                // 如果无法解析为整数，则返回一个默认值，或者抛出异常，取决于你的需求
                return 0; // 或者抛出异常：throw new RuntimeException("Invalid numeric part: " + numericPart);
            }
        }
        return 0; // 或者抛出异常，取决于你的需求
    }

    public boolean isLibFunc(){
        switch (funcName){
            case "@getint","@putch","@putint" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public boolean isPutch(){
        return funcName.equals("@putch");
    }

    public String getName() {
        return funcName;
    }

    public void setRefCounter(Map<Value, Integer> counter){
        refCounter = new LinkedList<>();
        PriorityQueue<Map.Entry<Value, Integer>> priorityQueue = new PriorityQueue<>(
                Map.Entry.comparingByValue(Comparator.reverseOrder())
        );
        priorityQueue.addAll(counter.entrySet());
        while (!priorityQueue.isEmpty()) {
            Map.Entry<Value, Integer> entry = priorityQueue.poll();
            refCounter.offer(entry.getKey());
        }
    }

    public void setConflictGraph(Map<Value, Set<Value>> conflictGraph){
        this.conflictGraph = conflictGraph;
    }

    public Queue<Value> getRefCounter(){
        return refCounter;
    }

    public Map<Value, Set<Value>> getConflictGraph(){
        return conflictGraph;
    }

    public String getMIPSName() {
        return funcName.substring(1);
    }

}