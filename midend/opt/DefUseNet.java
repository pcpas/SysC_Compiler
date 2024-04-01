package midend.opt;

import midend.Value.BasicBlock;
import midend.Value.Instructions.Instruction;
import midend.Value.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefUseNet {
    private Map<BasicBlock, DefUseNode> map = new HashMap<>();

    public DefUseNode getNode(BasicBlock block) {
        if (!map.containsKey(block))
            map.put(block, new DefUseNode(block));
        return map.get(block);
    }

    public Map<Instruction, BasicBlock> getDefiner(Value value,Instruction inst, BasicBlock block) {
        //System.out.println("======get definer: "+value + " ======");
        Map<Instruction, BasicBlock> definer = new HashMap<>();
        Set<BasicBlock> visited = new HashSet<>();
        DefUseNode node = getNode(block);
        if (node.findDef(value, inst) != null){
            definer.put(node.findDef(value, inst), block);
        }else {
            for (BasicBlock pre : block.getPredecessors()) {
                DefUseNode n = getNode(pre);
                if (n.getOut().contains(value)) {
                    getDefinerInAll(visited, value, pre, definer);
                }
            }
        }
        //System.out.println("===================================");
        return definer;
    }

    private void getDefinerInAll(Set<BasicBlock> visited, Value value, BasicBlock block, Map<Instruction, BasicBlock> definer) {
        if (visited.contains(block))
            return;
        visited.add(block);
        DefUseNode node = getNode(block);
        //System.out.println("visit block "+block);
        if (node.findDef(value, null) != null) {
            //System.out.println("i'm "+ block + " and get definer of "+value +" "+ node.findDef(value, inst));
            definer.put(node.findDef(value, null), block);
        } else {
            //System.out.println("i'm "+block + " and my pre: "+block.getPredecessors());
            for (BasicBlock pre : block.getPredecessors()) {
                //System.out.println("candidate pre block "+pre);

                DefUseNode n = getNode(pre);
                //System.out.println("out: "+n.getOut());
                if (n.getOut().contains(value)) {
                    //System.out.println("try to visit "+pre);
                    getDefinerInAll(visited, value, pre, definer);
                }
            }
        }
    }

    private void killInst(Instruction inst){

    }
}
