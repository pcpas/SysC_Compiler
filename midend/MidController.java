package midend;

import midend.opt.IROptimizer;
import frontend.tree.CompUnit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class MidController {
    private final BufferedWriter outputWriter = new BufferedWriter(new FileWriter("llvm_ir.txt"));
    private final BufferedWriter op_outputWriter = new BufferedWriter(new FileWriter("llvm_ir_op.txt"));
    private Visitor visitor;
    private IROptimizer irOptimizer;

    public MidController() throws IOException {
    }

    public void init(CompUnit compUnit) {
        IRBuildFactory irBuildFactory = new IRBuildFactory();
        this.visitor = new Visitor(compUnit, irBuildFactory, outputWriter);
    }

    public IRModule generateLLVM() throws IOException {
        return visitor.generateLLVM();
    }

    public IRModule optimize(IRModule module) throws IOException {
        irOptimizer = new IROptimizer(module, op_outputWriter);
        return irOptimizer.optimize();
    }

    public void close() throws IOException {
        outputWriter.close();
        op_outputWriter.close();
    }
}
