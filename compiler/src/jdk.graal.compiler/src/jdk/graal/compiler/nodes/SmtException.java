package jdk.graal.compiler.nodes;

public class SmtException extends RuntimeException {
    public SmtException(String message) {
        super("Unallowed SMT Representation: " + message);
    }
    public SmtException(String left, String right) {
        super("Unallowed SMT Representation: [Left] " + left + " [Right] " + right);
    }
}
