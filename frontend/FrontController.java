package frontend;

import exception.ExceptionHandler;

import java.io.*;

public class FrontController {
    private final BufferedReader testfileReader = new BufferedReader(new FileReader("testfile.txt"));
    private final BufferedWriter outputWriter = new BufferedWriter(new FileWriter("output.txt"));
    private final BufferedWriter errorWriter = new BufferedWriter(new FileWriter("error.txt"));
    private Tokenizer tokenizer;
    private Scanner scanner;
    private Tokens tokens;
    private Parser parser;
    private ExceptionHandler exceptionHandler;

    public FrontController() throws IOException {
    }

    public void initCompiler() throws IOException {
        scanner = new Scanner(outputWriter);
        tokens = new Tokens();
        exceptionHandler = new ExceptionHandler(errorWriter);
        tokenizer = new Tokenizer(tokens, scanner, testfileReader);
        parser = new Parser(scanner, exceptionHandler, outputWriter);
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    public Parser getParser() {
        return parser;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public Tokens getTokens() {
        return tokens;
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

}
