package jdk.graal.compiler.smt;

/**
 * Enumeration of floating-point operations used in the SMT encoding.
 * Binary operations: {@code FADD}, {@code FSUB}, {@code FMUL}, {@code FDIV}.
 * Unary operations: {@code FNEG}, {@code FABS}, {@code FSQRT}.
 * @author Jakub Wolek
 */
public enum FpOp {
    // Binary
    FADD, FSUB, FMUL, FDIV,
    // Unary
    FNEG, FABS, FSQRT
}