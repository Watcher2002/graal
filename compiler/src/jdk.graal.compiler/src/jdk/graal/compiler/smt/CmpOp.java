package jdk.graal.compiler.smt;

/**
 * Enumeration of comparison operations used in the SMT encoding.
 * Includes equality ({@code EQ}/{@code NE}) and signed/unsigned ordered comparisons
 * ({@code SLT}, {@code SLE}, {@code SGT}, {@code SGE}, {@code ULT}, {@code ULE}, {@code UGT}, {@code UGE}).
 * @author Jakub Wolek
 */
public enum CmpOp {
    EQ, NE, SLT, SLE, SGT, SGE, ULT, ULE, UGT, UGE
}
