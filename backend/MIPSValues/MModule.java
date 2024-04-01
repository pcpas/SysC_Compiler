package backend.MIPSValues;

import backend.MIPSInst.MInstruction;
import backend.MIPSTable.MTable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MModule {
    private final List<MFunction> functionList = new ArrayList<>();
    private final List<MGlobalVar> globalVarList = new ArrayList<>();

    private final MTable table;

    public MModule(MTable root) {
        table = root;
    }

    protected void addFunction(MFunction function) {
        functionList.add(function);
    }

    protected void addGlobalVar(MGlobalVar globalVar) {
        globalVarList.add(globalVar);
    }

    public List<MGlobalVar> getGlobalVarList() {
        return globalVarList;
    }

    public List<MFunction> getFunctionList() {
        return functionList;
    }

    public void printMIPSModule(BufferedWriter writer) throws IOException {
        //全局变量
        writer.write(".data\n");
        for (MGlobalVar var : this.getGlobalVarList()) {
            writer.write(var.toString());
            writer.newLine();
        }
        //全局代码
        //入口跳转至main函数
        writer.write(".text\n");
        //writer.write("\tjal main\n");
        //构建函数
        MFunction main = functionList.get(functionList.size()-1);
        for (MBasicBlock block : main.getBLockList()) {
            writer.write(block.toString());
            writer.newLine();
            for (MInstruction inst : block.getInstList()) {
                writer.write("\t" + inst.toString());
                writer.newLine();
            }
        }

        for (MFunction function : this.getFunctionList()) {
            if(function!=main)
                for (MBasicBlock block : function.getBLockList()) {
                    writer.write(block.toString());
                    writer.newLine();
                    for (MInstruction inst : block.getInstList()) {
                        writer.write("\t" + inst.toString());
                        writer.newLine();
                    }
                }
        }
    }
}