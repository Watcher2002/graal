package jdk.graal.compiler.smt;

/**
 * Selects how the SMT equivalence check is formulated.
 * {@code FLAT_BV} encodes everything as bit vectors, {@code MIXED_BVFP} additionally
 * supports floating-point operations, and {@code INCREMENTAL_CF} augments the check
 * with path conditions gathered from control-flow guards.
 * @author Jakub Wolek
 */
public enum VerificationStrategy {
    FLAT_BV, MIXED_BVFP, INCREMENTAL_CF
}