import midend.IRModule;
import backend.BackController;
import backend.MIPSValues.MModule;
import exception.ExceptionHandler;
import exception.SysYException;
import frontend.FrontController;
import frontend.Parser;
import frontend.Scanner;
import frontend.Tokenizer;
import frontend.tree.CompUnit;
import midend.MidController;

import java.io.IOException;

//version: 0.1.1
public class Compiler {

    public static void testControl(int stage, boolean opt) throws IOException, SysYException {
        FrontController fac = new FrontController();
        fac.initCompiler();
        System.out.println("Phase 1: tokenize");
        Tokenizer tokenizer = fac.getTokenizer();
        tokenizer.lexicalAnalyse();
        System.out.println("Phase 1 done");
        if (stage == 1) {
            Scanner scanner = fac.getScanner();
            scanner.printAllTokens();
            return;
        }
        System.out.println("Phase 2: syntax analyse");
        Parser parser = fac.getParser();
        CompUnit compUnit = parser.syntaxAnalyse(true);
        if (compUnit == null) {
            System.out.println("Parse failed....");
            return;
        }
        System.out.println("Phase 2 done");
        parser.printSyntaxTree();
        if (stage == 2) {
            return;
        }
        System.out.println("Phase 3: error detection");
        ExceptionHandler exceptionHandler = fac.getExceptionHandler();
        exceptionHandler.analyseError(compUnit);
        if (exceptionHandler.size() > 0) {
            exceptionHandler.printErrors();
            System.out.println("Error detected. Please fix all errors in error.txt and then try again.");
            return;
        }
        System.out.println("Phase 3 done");
        if (stage == 3) {
            return;
        }
        System.out.println("Phase 4: IR generation");
        MidController midController = new MidController();
        midController.init(compUnit);
        IRModule module = midController.generateLLVM();
        System.out.println("Phase 4 done");
        if(opt){
            System.out.println("=======Phase 4-1: IR generation optimize=======");
            module = midController.optimize(module);
            System.out.println("==============Phase 4-1 done==============");
        }
        midController.close();
        if (stage == 4)
            return;
        System.out.println("Phase 5: MIPS generation");
        BackController backController = new BackController(module);
        MModule m_module = backController.generateMIPS();
        System.out.println("Phase 5 done, congratulations!");
        if(opt){
            System.out.println("=======Phase 5-1: MIPS optimize=======");
            backController.optimize(m_module);
            System.out.println("==============Phase 5-1 done==============");
        }
        backController.close();
    }

    public static void main(String[] args) throws IOException, SysYException {
        testControl(5, true);
    }
}
