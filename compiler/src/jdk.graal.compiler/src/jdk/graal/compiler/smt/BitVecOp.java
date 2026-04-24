package jdk.graal.compiler.smt;

public enum BitVecOp {
    // Arithmetic
    ADD, SUB, MUL, SDIV, UDIV, SREM, UREM, NEG,
    // Bitwise
    AND, OR, XOR, NOT,
    // Shifts
    SHL, ASHR, LSHR
}