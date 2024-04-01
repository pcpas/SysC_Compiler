package midend;

import midend.Value.BasicBlock;
import midend.Value.Function;
import midend.Value.GlobalVar;
import midend.Value.Instructions.Instruction;
import midend.Value.Instructions.RetInst;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IRModule {
    private final List<Function> functionList;
    private final List<GlobalVar> globalVarList;
    private Function mainFunc;

    public IRModule(){
        functionList = new ArrayList<>();
        globalVarList = new ArrayList<>();
    }

    public IRModule(List<GlobalVar> globalVarList, List<Function> functionList){
        this.functionList = functionList;
        this.globalVarList = globalVarList;
    }
    public List<Function> getFunctionList() {
        return functionList;
    }

    public List<GlobalVar> getGlobalVarList() {
        return globalVarList;
    }

    public void addFunction(Function function) {
        if(function.getName().equals("@main"))
            mainFunc = function;
        functionList.add(function);
    }

    public void addGlobalVar(GlobalVar globalVar) {
        globalVarList.add(globalVar);
    }

    public Function getMainFunc(){
        if(mainFunc==null)
        {
            for(Function function : functionList){
                if(function.getName().equals("@main")){
                    mainFunc = function;
                    break;
                }
            }
        }
        return mainFunc;
    }

    public void printModule(BufferedWriter writer) throws IOException {
        writer.write("declare i32 @getint()\n" +
                "declare void @putint(i32)\n" +
                "declare void @putch(i32)\n" +
                "declare void @putstr(i8*)\n");

        //Global Var
        for (GlobalVar var : this.getGlobalVarList()) {
            writer.write(var.toDecl());
            writer.newLine();
        }
        writer.newLine();
        //Function
        for (Function function : this.getFunctionList()) {
            writer.write(function.toString() + "{");
            writer.newLine();
            //System.out.println(function.getBlocks());
            function.sortBlocksByLabel();
            boolean outLabel = false;
            int size = function.getBlocks().size();
            BasicBlock lastBlock = null;
            for(int i=0;i<size;i++){
                BasicBlock block = function.getBlocks().get(i);
                block.handleEndInst();
                if (outLabel)
                    writer.write(block + ":\n");
                else outLabel = true;
                for (Instruction instruction : block.getInstList()) {
                    writer.write("\t" + instruction);
                    writer.newLine();
                }
//                if (block.getEndInst() != null) {
//                    writer.write("\t" + block.getEndInst());
//                    writer.newLine();
//                }
                if(i==size-1)
                    lastBlock = block;
            }
            //如果最后一个函数没有结束语句要补一个
            int instSize = lastBlock.getInstList().size();
            if(lastBlock.getEndInst()==null){
                if(instSize==0 || (!(lastBlock.getInstList().get(instSize-1) instanceof RetInst))) {
                    writer.write("\tret void\n");
                }
            }
            writer.write("}\n");
        }
    }
}
