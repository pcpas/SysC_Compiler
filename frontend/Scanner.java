package frontend;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Scanner {
    private final ArrayList<Tokens.Token> tokens = new ArrayList<>();
    private BufferedWriter bufferedWriter;
    private Tokens.Token token;

    public Scanner() {
    }

    public Scanner(BufferedWriter bufferedWriter) {
        this.bufferedWriter = bufferedWriter;
    }

    public void putToken(Tokens.Token t) {
        //System.out.println("put "+t.getTag()+" "+t.getValue()+" "+t.getLineLocated());
        tokens.add(t);
    }

    public Tokens.Token next() {
        if (!tokens.isEmpty()) {
            token = tokens.remove(0);
        }
        return token;
    }

    public Tokens.Token lookAhead(int s) {
        if (s <= 0)
            return token;
        return tokens.get(s - 1);
    }

    public Tokens.Token getToken() {
        return token;
    }

    public Tokens.TokenKind getTokenKind() {
        return token.getKind();
    }

    public void printAllTokens() throws IOException {
        for (Tokens.Token token : tokens) {
            bufferedWriter.write(token.getTag() + " " + token.getValue());
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
    }

}
