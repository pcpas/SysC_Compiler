package midend.opt;

import java.util.ArrayList;
import java.util.List;

import static midend.opt.DagNode.DAGType.*;

public class DagNode {
    public enum DAGType{
        VALUE,ADD, MUL, SDIV, SREM, SUB, GETPTR, ZEXT, ALLOC, LOAD, STORE, LIBCALL
        ,GRE,GEQ,EQL,NEQ;
    }
    private DAGType type;
    private List<DagNode> sons = null;
    private final List<DagNode> parents = new ArrayList<>();
    private final int index;
    public DagNode(int index){
        type = DAGType.VALUE;
        this.index = index;
    }


    public DagNode(DAGType type ,int index, List<DagNode> nodes){
        this.type = type;
        this.index = index;
        this.sons = nodes;
        for(DagNode node : nodes){
            node.parents.add(this);
        }
    }

    public int getIndex() {
        return index;
    }

    public List<DagNode> getParents() {
        return parents;
    }

    public List<DagNode> getSons() {
        return sons;
    }

    public boolean isSameOp(DagNode.DAGType type, List<Integer> indexs){
        if(this.type != DAGType.VALUE && this.type == type){
            boolean res;
            switch (type){
                case ADD,MUL -> {
                    res = (sons.get(0).getIndex() == indexs.get(0) &&sons.get(1).getIndex() == indexs.get(1))||(sons.get(0).getIndex() == indexs.get(1) &&sons.get(1).getIndex() == indexs.get(0));
                }
                case GETPTR,ZEXT,LOAD,STORE,LIBCALL -> {
                    if(indexs.size()==sons.size()){
                        res = true;
                        for(int j=0;j<indexs.size();j++){
                            if(sons.get(j).getIndex()!=indexs.get(j)){
                                res = false;
                                break;
                            }
                        }
                    }else res = false;
                }
                case ALLOC -> res = true;
                default -> {
                    res = sons.get(0).getIndex() == indexs.get(0) &&sons.get(1).getIndex() == indexs.get(1);
                }
            }
            return res;
        }
        return false;
    }

    @Override
    public String toString(){
        if(type == DAGType.VALUE){
            return "leaf "+index;
        }else {
                StringBuilder builder = new StringBuilder();
                builder.append(type).append(" ").append(index).append(" sons: ");
                for(DagNode node : sons){
                    builder.append(node.getIndex()).append(" ");
                }
                return builder.toString();
        }
    }

    public boolean isLeaf(){
        return type==VALUE;
    }

    public boolean isGlobal(){
        return type==ALLOC || type==LOAD || type==STORE || type == GETPTR || type == LIBCALL;
    }


    public void clearGlobal(){

    }

}
