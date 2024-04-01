package backend.MIPSValues;

import midend.Value.BasicBlock;
import backend.MIPSInst.MInstruction;

import java.util.ArrayList;
import java.util.List;

public class MBasicBlock implements MValue {
    private List<MInstruction> MInstructionList;
    private final String name;
    private MFunction parent;

    private BasicBlock block;


    public MBasicBlock(MFunction function, String name, BasicBlock block) {
        function.addBasicBlock(this);
        parent = function;
        this.MInstructionList = new ArrayList<>();
        this.name = name;
        this.block = block;
    }

    public BasicBlock getIRblock(){
        return block;
    }

    public MFunction getFunction() {
        return parent;
    }

    public void setMInstructionList(List<MInstruction> list){
        MInstructionList = list;
    }

    public void addInst(MInstruction MInstruction) {
        MInstructionList.add(MInstruction);
    }

    public List<MInstruction> getInstList() {
        return MInstructionList;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + ":";
    }

    private final List<MBasicBlock> predecessors = new ArrayList<>();
    private final List<MBasicBlock> successors = new ArrayList<>();

    public void addPredecessor(MBasicBlock block){
        predecessors.add(block);
    }

    public void addSuccessors(MBasicBlock block){
        successors.add(block);
    }

    public List<MBasicBlock> getPredecessors() {
        return predecessors;
    }

    public List<MBasicBlock> getSuccessors() {
        return successors;
    }

    public void combineWithBlock(MBasicBlock block){
        //删除跳转指令
        int instSize = MInstructionList.size();
        MInstructionList.remove(instSize-1);
        //合并指令
        MInstructionList.addAll(block.getInstList());
        //合并边
        successors.clear();
        successors.addAll(block.getSuccessors());
    }

    public void MoveLastNInstToTopK(int n, int k){
        if(n==0) return;
        List<MInstruction> lastNInstructions = new ArrayList<>(MInstructionList.subList(MInstructionList.size() - n, MInstructionList.size()));
        MInstructionList.removeAll(lastNInstructions);
        MInstructionList.addAll(k, lastNInstructions);
    }
}
