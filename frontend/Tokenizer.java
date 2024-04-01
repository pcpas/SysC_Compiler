package frontend;

import exception.SysYException;

import java.io.BufferedReader;
import java.io.IOException;

public class Tokenizer {
    private final Tokens tokens;
    private final Scanner scanner;
    private final BufferedReader bufferedReader;
    private final StringBuilder sb;
    private int charRead;
    private int curLine;

    Tokenizer(Tokens tokens, Scanner scanner, BufferedReader bufferedReader) {
        this.tokens = tokens;
        this.scanner = scanner;
        this.bufferedReader = bufferedReader;
        this.sb = new StringBuilder();
        this.curLine = 1;
    }

    private boolean isOperator(int c) {
        return switch (c) {
            case '+', '-', '*', '%', ';', ',', '(', ')', '[', ']', '{', '}' -> true;
            default -> false;
        };
    }

    private boolean isBooleanOperator(int c) {
        return switch (c) {
            case '!', '>', '<', '=' -> true;
            default -> false;
        };
    }

    private int next() throws IOException, SysYException {
        int tc;
        tc = bufferedReader.read();
        if (tc == -1) {
            throw new SysYException(SysYException.ExceptionKind.END_OF_FILE, curLine);
        }
        return tc;
    }

    private void scanIdent() throws IOException, SysYException {
        sb.appendCodePoint(charRead);
        // read whole word
        try {
            while (true) {
                bufferedReader.mark(2);
                charRead = next();
                if (charRead == '_' || Character.isLetter(charRead) || Character.isDigit(charRead)) {
                    sb.appendCodePoint(charRead);
                } else {
                    bufferedReader.reset();
                    break;
                }
            }
        } finally {
            if (tokens.isExist(sb.toString())) {
                scanner.putToken(new Tokens.Token(sb.toString(), curLine));
            } else {
                scanner.putToken(new Tokens.Token(Tokens.TokenKind.IDENT, sb.toString(), curLine));
            }
            sb.setLength(0);
        }
    }

    private void scanDigit() throws IOException, SysYException {
        sb.appendCodePoint(charRead);
        try {
            while (true) {
                bufferedReader.mark(2);
                charRead = next();
                if (Character.isDigit(charRead)) {
                    sb.appendCodePoint(charRead);
                    bufferedReader.mark(2);
                } else {
                    bufferedReader.reset();
                    break;
                }
            }
        } finally {
            scanner.putToken(new Tokens.Token(Tokens.TokenKind.INTCONST, sb.toString(), curLine));
            sb.setLength(0);
        }

    }


    private void scanFmtString() throws IOException, SysYException {
        sb.appendCodePoint(charRead);
        do {
            charRead = next();
            sb.appendCodePoint(charRead);
        } while (charRead != '"');
        scanner.putToken(new Tokens.Token(Tokens.TokenKind.FMTSTRING, sb.toString(), curLine));
        sb.setLength(0);
    }

    private void scanOperator(String op) {
        //System.out.println(op);
        scanner.putToken(new Tokens.Token(op, curLine));
    }

    private void scanBooleanOperator() throws IOException, SysYException {
        sb.appendCodePoint(charRead);
        try {
            bufferedReader.mark(2);
            charRead = next();
            if (charRead == '=') {
                sb.appendCodePoint(charRead);
            } else {
                bufferedReader.reset();
            }
        } finally {
            scanOperator(sb.toString());
            sb.setLength(0);
        }
    }

    private void scanComment() throws IOException, SysYException {
        bufferedReader.mark(2);
        charRead = next();
        if (charRead == '/') {
            do {
                charRead = next();
            } while (charRead != '\n');
            curLine++;
        } else if (charRead == '*') {
            int step = 1;
            while (true) {
                charRead = next();
                if (charRead == '\n')
                    curLine++;
                if (step == 1) {
                    if (charRead == '*') {
                        step = 2;
                    }
                } else {
                    if (charRead == '/') {
                        break;
                    } else if (charRead == '*') {
                        continue;
                    } else {
                        step = 1;
                    }
                }
            }
        } else {
            bufferedReader.reset();
            scanOperator("/");
        }
    }

    public void lexicalAnalyse() throws IOException {
        Tokens.Token token;
        int charBuffer = -1;
        try {
            while (true) {
                charRead = next();
                //清除空白符
                if (Character.isWhitespace(charRead)) {
                    if (charRead == '\n')
                        curLine++;
                    continue;
                }

                if (charRead == '_' || Character.isLetter(charRead)) {
                    scanIdent();
                } else if (Character.isDigit(charRead)) {
                    scanDigit();
                } else if (charRead == '"') {
                    scanFmtString();
                } else if (charRead == '|') {
                    charRead = next();
                    if (charRead == '|') {
                        scanOperator("||");
                    } else {
                        throw new SysYException(SysYException.ExceptionKind.ERROR, curLine);
                    }
                } else if (charRead == '&') {
                    charRead = next();
                    if (charRead == '&') {
                        scanOperator("&&");
                    } else {
                        throw new SysYException(SysYException.ExceptionKind.ERROR, curLine);
                    }
                } else if (isOperator(charRead)) {
                    scanOperator(Character.toString(charRead));
                } else if (isBooleanOperator(charRead)) {
                    scanBooleanOperator();
                } else if (charRead == '/') {
                    scanComment();
                } else {
                    throw new SysYException(SysYException.ExceptionKind.ERROR, curLine);
                }
            }
        } catch (SysYException e) {
            //错误处理
        }
    }


}
