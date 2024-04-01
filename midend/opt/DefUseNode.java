package midend.opt;

import midend.Value.BasicBlock;
import midend.Value.Instructions.Instruction;
import midend.Value.Value;

import java.util.*;

public class DefUseNode {
    private final Set<Value> use = new HashSet<>();
    private final Set<Value> def = new HashSet<>();
    private final Set<Value> in = new HashSet<>();
    private final Set<Value> out = new HashSet<>();
    private BasicBlock block;
    private Map<Value, List<DU>> defUseList = new HashMap<>();
    private Map<Value, Map<Instruction, Integer>> instContain = new HashMap<>();

    public DefUseNode(BasicBlock block) {
        this.block = block;
    }

    public Set<Value> getUse() {
        return use;
    }

    public Set<Value> getDef() {
        return def;
    }

    public Set<Value> getIn() {
        return in;
    }

    public Set<Value> getOut() {
        return out;
    }

    public void insertDef(Value value, Instruction inst) {
        if(!defUseList.containsKey(value)){
            defUseList.put(value, new ArrayList<>());
        }
        int index = defUseList.get(value).size();
        defUseList.get(value).add(new DU(inst, 1));

        if(!instContain.containsKey(value)){
            instContain.put(value, new HashMap<>());
        }
        instContain.get(value).put(inst, index);
    }

    public void insertUse(Value value, Instruction inst){
        if(!defUseList.containsKey(value)){
            defUseList.put(value, new ArrayList<>());
        }
        int index = defUseList.get(value).size();
        defUseList.get(value).add(new DU(inst, 0));
        if(!instContain.containsKey(value)){
            instContain.put(value, new HashMap<>());
        }
        instContain.get(value).put(inst, index);
    }

    public Instruction findDef(Value value, Instruction inst) {
        if(defUseList.get(value)==null)
            return null;
        int i = instContain.containsKey(value) && instContain.get(value).containsKey(inst)?instContain.get(value).get(inst)-1 : defUseList.get(value).size()-1;
        for(;i>=0;i--){
            DU du = defUseList.get(value).get(i);
            if(du.isDef()){
                return du.getInst();
            }
        }
        return null;
    }
}
