package jdk.graal.compiler.smt;

import com.microsoft.z3.Model;

public sealed interface VerificationResult
        permits VerificationResult.Valid, VerificationResult.Counterexample {

    record Valid() implements VerificationResult {
    }

    record Counterexample(String formula, Model model) implements VerificationResult {
    }
}