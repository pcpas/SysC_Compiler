package backend;

import backend.MIPSInst.*;
import backend.MIPSValues.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MipsOptimizer {
    private final BufferedWriter writer;
    private final MModule module;

    private int uniqueCnt = 0;

    public MipsOptimizer(BufferedWriter writer, MModule module) {
        this.writer = writer;
        this.module = module;
    }

    public void optimize() throws IOException {
        List<MFunction> functions= module.getFunctionList();
        for(MFunction function : functions){
            System.out.println("======Function："+function.getName()+"======");
            combineBasicBlocks(function);
            combineAllPutch(function);
            System.out.println("------Kill：useless code------");
            killUselessCode(function);
            System.out.println("------Kill：useless JInst------");
            killUselessJ(function);
            System.out.println("------Combine：Li Move------");
            combineLiMoveInst(function);
            System.out.println("------Combine：Move All------");
            combineMoveAll1(function);
            combineMoveAll2(function);
            combineMoveAll1(function);
            combineMoveAll2(function);
            combineMoveAll1(function);
            combineMoveAll2(function);
        }
        module.printMIPSModule(writer);
    }

    private void combineMoveAll1(MFunction function) {
        for(MBasicBlock block : function.getBLockList()){
            List<MInstruction> list = new ArrayList<>();
            for(int i = 0;i<block.getInstList().size();i++){
                MInstruction inst = block.getInstList().get(i);
                boolean isOk = true;
                if(inst instanceof IInst ii1 && ii1.operation == IInst.Operation.MOVE && !(ii1.rs instanceof Register r&& r.isMyMagic())){
                    //System.out.println("enter "+inst);
                    for(int j = i+1;j<block.getInstList().size();j++){
                        MInstruction inst2 = block.getInstList().get(j);
                        if(inst2 instanceof IInst ii2 && ii2.operation!= IInst.Operation.LW && ii2.operation!= IInst.Operation.BNEZ && ii2.getModified()!= Register.R4){
                            if(ii1.rs == ii2.rt && !(ii2.getModified() instanceof Register r&& r.isMyTemp())){
                                boolean isOk2 = true;
                                for(int k = i+1;k<j;k++){
                                    MInstruction inst3 = block.getInstList().get(k);
                                    if(inst3.getModified() == ii2.getModified() || inst3.getUsed().contains(ii2.getModified())){
                                        isOk2 = false;
                                        break;
                                    }
                                }
                                if(isOk2){
                                    block.getInstList().remove(ii2);
                                    IInst ii3 = new IInst(block, ii2.operation, ii2.rs, ii1.rt, ii2.immediate);
                                    block.getInstList().remove(ii3);
                                    list.add(ii3);
                                    isOk = false;
                                    System.out.println("combine1: "+ii1+" and "+ii2);
                                    break;
                                }
                            }
                        }
                        else if (inst2 instanceof RInst ri2) {
                            if(ri2.getUsed().contains(ii1.getModified())){
                                boolean isOk2 = true;
                                for(int k = i+1;k<j;k++){
                                    MInstruction inst3 = block.getInstList().get(k);
                                    if(inst3.getModified() == ri2.getModified() || inst3.getUsed().contains(ri2.getModified()) || ri2.getUsed().contains(inst3.getModified())){
                                        isOk2 = false;
                                        break;
                                    }
                                }
                                if(isOk2){
                                    block.getInstList().remove(ri2);
                                    MValue rs = ii1.rs==ri2.rs?ii1.rt:ri2.rs;
                                    MValue rt = ii1.rs==ri2.rt?ii1.rt:ri2.rt;
                                    RInst ii3 = new RInst(block, ri2.operation, ri2.getModified(), rs,rt);
                                    block.getInstList().remove(ii3);
                                    list.add(ii3);
                                    isOk = false;
                                    System.out.println("combine2: "+ii1+" and "+ri2);
                                    break;
                                }
                            }
                        }
                        if(inst2 instanceof JInst || inst2.getModified()==ii1.rt || inst2.getModified()==ii1.rs){
                            break;
                        }
                    }
                }
                if(isOk)
                    list.add(inst);
            }
            block.getInstList().clear();
            block.getInstList().addAll(list);
        }
    }

    private void combineMoveAll2(MFunction function) {
        for(MBasicBlock block : function.getBLockList()){
            List<MInstruction> list = new ArrayList<>();
            for(int i = 0;i<block.getInstList().size();i++){
                MInstruction inst = block.getInstList().get(i);
                boolean isOk = true;
                if(inst instanceof IInst ii1 && ii1.operation == IInst.Operation.MOVE && !(ii1.rs instanceof Register r&& r.isMyMagic())){
                    //System.out.println("enter "+inst);
                    for(int j = i+1;j<block.getInstList().size();j++){
                        MInstruction inst2 = block.getInstList().get(j);
                        if(inst2 instanceof IInst ii2
                                && ii1.rs == ii2.rt
                                && ii2.operation!= IInst.Operation.LW
                                && ii2.operation!= IInst.Operation.BNEZ
                                && !(ii2.getModified() instanceof Register r&& r.isMyTemp())){
                            List<MInstruction> interInsts = new ArrayList<>();
                            for(int k = i+1;k<j;k++){
                                MInstruction inst3 = block.getInstList().get(k);
                                interInsts.add(inst3);
                            }
                            IInst ii3 = new IInst(block, ii2.operation, ii2.rs, ii1.rt, ii2.immediate);
                            block.getInstList().remove(ii3);
                            list.addAll(interInsts);
                            list.add(ii3);
                            i = j;
                            isOk = false;
                            System.out.println("combine3: "+ii1+" and "+ii2);
                            break;
                        }
                        else if (inst2 instanceof RInst ri2 && ri2.getUsed().contains(ii1.getModified())) {
                            List<MInstruction> interInsts = new ArrayList<>();
                            for(int k = i+1;k<j;k++){
                                MInstruction inst3 = block.getInstList().get(k);
                                interInsts.add(inst3);
                            }
                            MValue rs = ii1.rs==ri2.rs?ii1.rt:ri2.rs;
                            MValue rt = ii1.rs==ri2.rt?ii1.rt:ri2.rt;
                            RInst ii3 = new RInst(block, ri2.operation, ri2.getModified(), rs,rt);
                            block.getInstList().remove(ii3);
                            list.addAll(interInsts);
                            list.add(ii3);
                            isOk = false;
                            i = j;
                            System.out.println("combine4: "+ii1+" and "+ri2);
                            break;
                        }
                        if(inst2 instanceof JInst || inst2.getModified()==ii1.rt || inst2.getModified()==ii1.rs || inst2.getUsed().contains(ii1.rs)){
                            break;
                        }
                    }
                }
                if(isOk)
                    list.add(inst);
            }
            block.getInstList().clear();
            block.getInstList().addAll(list);
        }
    }

    //
//    private void combineMoveStore(MFunction function) {
//        for(MBasicBlock block : function.getBLockList()){
//            List<MInstruction> list = new ArrayList<>();
//            for(int i = 0;i<block.getInstList().size();i++){
//                MInstruction inst = block.getInstList().get(i);
//                boolean isOk = true;
//                if(inst instanceof IInst ii1 && ii1.operation== IInst.Operation.MOVE){
//                    for(int j = i+1;j<block.getInstList().size();j++){
//                        MInstruction inst2 = block.getInstList().get(j);
//                        if(inst2 instanceof IInst ii2 && ii2.operation == IInst.Operation.SW){
//                            if(ii1.rs == ii2.rt){
//                                boolean isOk2 = true;
//                                for(int k = i+1;k<j;k++){
//                                    MInstruction inst3 = block.getInstList().get(k);
//                                    if(inst3.rs == ii2.rs){
//                                        isOk2 = false;
//                                        break;
//                                    }
//                                }
//                                if(!isOk2)
//                                    break;
//                                block.getInstList().remove(ii2);
//                                IInst ii3 = new IInst(block, IInst.Operation.SW, ii2.rs, ii1.rt, ii2.immediate);
//                                block.getInstList().remove(ii3);
//                                list.add(ii3);
//                                isOk = false;
//                                System.out.println("combine "+ii1+" and "+ii2);
//                                break;
//                            }
//                        }else {
//                            if(inst2 instanceof JInst || inst2.rs == ii1.rt || inst2.rs == ii1.rs)
//                                break;
//                        }
//                    }
//                }
//                if(isOk)
//                    list.add(inst);
//            }
//            block.getInstList().clear();
//            block.getInstList().addAll(list);
//        }
//    }

    private void combineLiMoveInst(MFunction function) {
        for(MBasicBlock block : function.getBLockList()){
            List<MInstruction> list = new ArrayList<>();
            for(int i = 0;i<block.getInstList().size();i++){
                MInstruction inst = block.getInstList().get(i);
                if(inst instanceof IInst ii1 && ii1.operation== IInst.Operation.LI){
                    if(i+1<block.getInstList().size())
                    {
                        MInstruction next = block.getInstList().get(i+1);
                        if(next instanceof IInst ii2 && ii2.operation== IInst.Operation.MOVE){
                            if(ii1.rs == ii2.rt){
                                IInst newI = new IInst(block, IInst.Operation.LI, ii2.rs, null, ii1.immediate);
                                list.add(newI);
                                block.getInstList().remove(newI);
                                System.out.println("combine "+ii1+" and "+ii2);
                                i++;
                                continue;
                            }
                        }
                    }
                }
                list.add(inst);
            }
            block.getInstList().clear();
            block.getInstList().addAll(list);
        }
    }

    private void combineTwoMoveInst(MFunction function) {
        for(MBasicBlock block : function.getBLockList()){
            List<MInstruction> list = new ArrayList<>();
            for(int i = 0;i<block.getInstList().size();i++){
                MInstruction inst = block.getInstList().get(i);
                if(inst instanceof IInst ii1 && ii1.operation== IInst.Operation.MOVE){
                    if(i+1<block.getInstList().size())
                    {
                        MInstruction next = block.getInstList().get(i+1);
                        if(next instanceof IInst ii2 && ii2.operation== IInst.Operation.MOVE){
                            if(ii1.rs == ii2.rt && ii1.rt == ii2.rs){
                                //这个条件很弱！有可能有bug
//                                IInst newI = new IInst(block, IInst.Operation.MOVE, ii2.rs, ii1.rt, null);
//                                list.add(newI);
//                                block.getInstList().remove(newI);
                                if(ii1.rs instanceof Register reg && (reg.isMyGlobal() || reg.isMyTemp()))
                                    list.add(ii1);
                                if(ii2.rs instanceof Register reg && (reg.isMyGlobal() || reg.isMyTemp()))
                                    list.add(ii2);
                                System.out.println("combine "+ii1+" and "+ii2);
                                i++;
                                continue;
                            }
                        }
                    }
                }
                list.add(inst);
            }
            block.getInstList().clear();
            block.getInstList().addAll(list);
        }
    }

    private void killUselessJ(MFunction function) {
        for(int i = 0;i<function.getBLockList().size();i++){
            MBasicBlock block = function.getBLockList().get(i);
            int j = block.getInstList().size()-1;
            if(j<0 || i+1>=function.getBLockList().size())
                continue;
            MInstruction inst = block.getInstList().get(j);
            MBasicBlock next = function.getBLockList().get(i+1);
            if(inst instanceof JInst ji && ji.isJ2Block(next)){
                block.getInstList().remove(inst);
                System.out.println("kill "+inst);
            }
//            if(block.getSuccessors().size()==1){
//                if(block.getSuccessors().get(0) == function.getBLockList().get(i+1)){
//                    int j = block.getInstList().size()-1;
//                    if(j<0)
//                        continue;
//                    MInstruction inst = block.getInstList().get(j);
//                    if(inst instanceof JInst ji && ji.isJ()){
//                        block.getInstList().remove(inst);
//                        System.out.println("kill "+inst);
//                    }
//                }
//            }
        }
    }

    public void combineAllPutch(MFunction function){
        for(MBasicBlock block : function.getBLockList()){
            List<MInstruction> list = new ArrayList<>();
            for(int i = 0;i<block.getInstList().size();i++){
                MInstruction inst = block.getInstList().get(i);
                if(inst instanceof IInst && ((IInst) inst).isPutchLI1()){
                    //System.out.println("debug: "+inst);
                    List<MInstruction> putstr = new ArrayList<>();
                    int size = block.getInstList().size();
                    StringBuilder builder = new StringBuilder();
                    builder.append("\"");
                    while (true){
                        putstr.add(inst);
                        i++;
                        if(i>=size)
                            break;
                        if(block.getInstList().get(i) instanceof IInst ii2 && ii2.isPutchLI2()){
                            IInst put = (IInst) block.getInstList().get(i++);
                            putstr.add(put);
                            MInstruction syscall = block.getInstList().get(i++);
                            putstr.add(syscall);
                            int cint = Integer.parseInt(((ImmediateNumber)put.getImmediate()).getValue());
                            String c = cint==10?"\\n": String.valueOf((char)cint);
                            builder.append(c);
                            if(i>=block.getInstList().size())
                                break;
                            MInstruction next = block.getInstList().get(i);
                            if(!(next instanceof IInst) || !((IInst) next).isPutchLI1()){
                                i--;
                                break;
                            }
                        }else {
                            i--;
                            break;
                        }
                    }
                    builder.append("\"");
                    if(builder.toString().length()>4){
                        putstr.clear();

                        MInstruction li = new IInst(block, IInst.Operation.LI, Register.R2, null, new ImmediateNumber("4"));
                        putstr.add(li);
                        block.getInstList().remove(li);
                        String name = getUniqueStringName();
                        new MGlobalVar(module, MGlobalVar.InitType.ASCII, name, List.of(builder.toString()));
                        MemAddr sMem = new MemAddr(new Label(name), new ImmediateNumber("0"));
                        MInstruction la = new IInst(block, IInst.Operation.LA, Register.R4, sMem.getBase(), null);
                        putstr.add(la);
                        block.getInstList().remove(la);
                        MInstruction sys = new Syscall(block);
                        putstr.add(sys);
                        block.getInstList().remove(sys);
                    }
                    list.addAll(putstr);
                }else
                    list.add(inst);
            }
            block.getInstList().clear();
            block.getInstList().addAll(list);
        }
    }

    private String getUniqueStringName(){
        return "global_s_"+uniqueCnt++;
    }

    private void combineBasicBlocks(MFunction function){
        List<MBasicBlock> blocks = function.getBLockList();
        for(int i=0;i<blocks.size();i++){
            MBasicBlock block = blocks.get(i);
            if(canCombineWithNext(block)){
                MBasicBlock next = block.getSuccessors().get(0);
                blocks.remove(next);
                block.combineWithBlock(next);
                i--;
            }
        }
    }

    private boolean canCombineWithNext(MBasicBlock block){
        if(block.getSuccessors().size() == 1){
            MBasicBlock nextBlock = block.getSuccessors().get(0);
            return nextBlock.getPredecessors().size() == 1;
        }
        return false;
    }

    private void killUselessCode(MFunction function){
        for(MBasicBlock block : function.getBLockList()){
            List<MInstruction> newInst = new ArrayList<>();
            for(MInstruction inst : block.getInstList()){
                if(inst instanceof IInst ii && ii.isDead()){
                    System.out.println("kill: "+ii);
                }
                else if(inst instanceof RInst ri && ri.isDead()){
                    System.out.println("kill: "+ri);
                }
                else {
                    newInst.add(inst);
                }
            }
            block.setMInstructionList(newInst);
        }
    }

//    private void visitAllBlocks(MBasicBlock block, Set<MBasicBlock> visitedBlocks){
//        if(visitedBlocks.contains(block))
//            return;
//        visitedBlocks.add(block);
//        System.out.println("block"+block.getName());
//        //System.out.println(block.);
//    }


}
