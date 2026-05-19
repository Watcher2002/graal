package jdk.graal.compiler.smt;

/**
 * Enumeration of bit-vector operations available in the SMT encoding.
 * Covers arithmetic ({@code ADD}, {@code SUB}, {@code MUL}, signed/unsigned division and remainder),
 * bitwise ({@code AND}, {@code OR}, {@code XOR}, {@code NOT}), and shift operations.
 * @author Jakub Wolek
 */
public enum BitVecOp {
    // Arithmetic
    ADD, SUB, MUL, SDIV, UDIV, SREM, UREM, NEG,
    // Bitwise
    AND, OR, XOR, NOT,
    // Shifts
    SHL, ASHR, LSHR
}