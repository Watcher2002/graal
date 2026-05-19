package jdk.graal.compiler.smt;

/**
 * Enumeration of boolean operations used in the SMT encoding:
 * unary {@code NOT} and binary {@code AND}, {@code OR}, {@code XOR}, {@code IMPLIES}.
 * @author Jakub Wolek
 */
public enum BoolOp {NOT, AND, OR, XOR, IMPLIES}
