package jdk.graal.compiler.smt;

/**
 * Thrown when the SMT solver returns {@code UNKNOWN} or an unexpected internal error
 * occurs during a canonicalization verification query.
 * @author Jakub Wolek
 */
public class VerificationException extends RuntimeException {
    public VerificationException(String message) {
        super(message);
    }
}
