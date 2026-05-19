package jdk.graal.compiler.smt;

import com.microsoft.z3.Model;

/**
 * Outcome of an SMT canonicalization verification query.
 * {@link Valid} indicates the rewrite is sound (Z3 returned UNSAT);
 * {@link Counterexample} holds a satisfying assignment that witnesses an unsound rewrite.
 * @author Jakub Wolek
 */
public sealed interface VerificationResult
        permits VerificationResult.Valid, VerificationResult.Counterexample {

    record Valid() implements VerificationResult {
    }

    record Counterexample(String formula, Model model) implements VerificationResult {
    }
}