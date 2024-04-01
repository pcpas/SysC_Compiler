package midend.Value;

import midend.Value.Instructions.BrInst;
import midend.Value.Instructions.Instruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasicBlock extends IRValue {
    private List<Instruction> instList;
    private Function parent;
    //仅预设的跳转语句
    private BrInst endInst = null;
    private boolean isEnd = false;
    private String label;
    private final List<BasicBlock> predecessors = new ArrayList<>();
    private final List<BasicBlock> successors = new ArrayList<>();

    private boolean inloop = false;

    private Set<Value> in;
    private Set<Value> out;

    public Set<Value> getIn() {
        return in;
    }

    public Set<Value> getOut() {
        return out;
    }

    public void setIn(Set<Value> s){
        in = s;
    }

    public void setOut(Set<Value> s){
        out = s;
    }

    public void setInLoop(){
        inloop = true;
    }

    public boolean isInLoop(){
        return inloop;
    }


    public BasicBlock(String label, Function parent) {
        this.label = label;
        this.parent = parent;
        instList = new ArrayList<>();
        parent.addBlock(this);
    }


    public BasicBlock(String label) {
        this.label = label;
        this.parent = null;
        instList = new ArrayList<>();
    }

    public List<Instruction> getInstList() {
        return instList;
    }

    public void setInstList(List<Instruction> list){
        instList = list;
    }

    public void addInst(Instruction instruction) {
        if(!isEnd){
            instList.add(instruction);
            if(instruction instanceof BrInst){
                setPSBlocks((BrInst) instruction);
            }
        }
    }

    public BrInst getEndInst() {
        return endInst;
    }

    public void setEndInst(BrInst inst) {
        if(!isEnd) {
            if(endInst!=null)
                removePSBlock(endInst);
            endInst = inst;
            setPSBlocks(inst);
        }
    }

    public void handleEndInst(){
        if(endInst!=null)
        {
            instList.add(endInst);
            endInst = null;
            isEnd = true;
        }
    }

    public void setBlockEnd(){
        isEnd = true;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMIPSLabel() {
        return parent.getMIPSName() + "_lzzql_" + label;
    }

    @Override
    public String toString() {
        return label;
    }

    private void setPSBlocks(BrInst inst){
        successors.add(inst.b1);
        inst.b1.predecessors.add(this);
        if(inst.b2!=null) {
            successors.add(inst.b2);
            inst.b2.predecessors.add(this);
        }
    }

    private void removePSBlock(BrInst inst){
        successors.remove(inst.b1);
        inst.b1.predecessors.remove(this);
        if(inst.b2!=null){
            successors.remove(inst.b2);
            inst.b2.predecessors.remove(this);
        }
    }

    public List<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public List<BasicBlock> getSuccessors() {
        return successors;
    }

    public void selfDelete(){
        for(BasicBlock block : successors){
            block.predecessors.remove(this);
        }
        for(BasicBlock block : predecessors){
            block.successors.remove(this);
        }
    }

    public Function getParent() {
        return parent;
    }

    public void setParent(Function function){
        parent = function;
    }

}
