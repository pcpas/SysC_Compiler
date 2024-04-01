package backend;

import midend.IRModule;
import backend.MIPSValues.MModule;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class BackController {
    private final MIPSBuilder mipsBuilder;
    private MipsOptimizer mipsOptimizer;
    private final BufferedWriter outputWriter;
    private BufferedWriter opt_outputWriter;

    public BackController(IRModule module) throws IOException {
        outputWriter = new BufferedWriter(new FileWriter("mips_ori.txt"));
        mipsBuilder = new MIPSBuilder(outputWriter, module);
    }

    public MModule generateMIPS() throws IOException {
        MModule res =  mipsBuilder.generateMIPS();
        outputWriter.close();
        return res;
    }

    public void optimize(MModule module) throws IOException {
        opt_outputWriter = new BufferedWriter(new FileWriter("mips.txt"));
        mipsOptimizer = new MipsOptimizer(opt_outputWriter, module);
        mipsOptimizer.optimize();
    }

    public void close() throws IOException {
        if(opt_outputWriter!=null)
            opt_outputWriter.close();
    }

}
