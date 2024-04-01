package midend.opt;

import midend.Value.Instructions.Instruction;
import midend.Value.Value;

import java.util.*;

public class DAG {
    Map<Value, Integer> table = new HashMap<>();
    Map<Integer, DagNode> graph = new HashMap<>();
    Map<DagNode, List<Instruction>> dagRef = new HashMap<>();
    int index = 0;

    public Integer getValueIndex(Value value){
        if(table.containsKey(value))
            return table.get(value);
        else {
            return insertLeaf(value);
        }
    }

    public int findValueIndex(Value value){
        if(table.get(value)!=null)
            return table.get(value);
        else return -1;
    }

    public void setValueIndex(Value value, int index){
        table.put(value, index);
    }

    public int insertLeaf(Value value){
        table.put(value, ++index);
        DagNode node = new DagNode(index);
        graph.put(index, node);
        return index;
    }

    public DagNode getNodeByIndex(int index){
        return graph.get(index);
    }

    public DagNode findMidNode(DagNode.DAGType type, int i, int j){
        for(DagNode node: graph.values())
        {
            if(node.isSameOp(type, Arrays.asList(i,j)))
                return node;
        }
        return null;
    }

    public DagNode findMidNode(DagNode.DAGType type, Instruction inst, List<Integer> indexs){
        for(DagNode node: graph.values())
        {
            if(node.isSameOp(type, indexs)){
                createInstRef(inst, node);
                return node;
            }
        }
        return null;
    }

    public DagNode createMidNode(DagNode.DAGType type, Instruction inst, List<Integer> indexs){
        List<DagNode> nodes = new ArrayList<>();
        for(int i: indexs){
            nodes.add(graph.get(i));
        }
        DagNode mid =  new DagNode(type, ++index ,nodes);
        createInstRef(inst, mid);
        graph.put(index, mid);
        return mid;
    }

    public void createInstRef(Instruction inst, DagNode mid){
        if(dagRef.containsKey(mid)){
            dagRef.get(mid).add(inst);
        }else {
            dagRef.put(mid, new ArrayList<>(List.of(inst)));
        }
    }

    public List<Instruction> getMidInst(DagNode node){
        return dagRef.get(node);
    }

    public List<Value> getAllSameValue(Value value){
        int index = table.get(value);
        List<Value> values = new ArrayList<>();
        for(Map.Entry<Value,Integer> entry : table.entrySet()){
            if(entry.getValue() == index && entry.getKey()!=value)
                values.add(entry.getKey());
        }
        return values;
    }

    public Map<Integer, DagNode> getGraph() {
        return graph;
    }

    public void printTable(){
        System.out.println("=============================");
        for(Map.Entry<Value, Integer> entry : table.entrySet()){
            System.out.println(entry.getKey().getName()+" "+entry.getValue());
        }
    }

    public void printGraph(){
        System.out.println("=============================");
        for(Map.Entry<Integer, DagNode> entry : graph.entrySet()){
            System.out.println(entry.getKey()+" "+entry.getValue());
        }
    }
}
