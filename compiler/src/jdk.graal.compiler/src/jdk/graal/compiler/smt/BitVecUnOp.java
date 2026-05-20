package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;

import java.util.List;

/**
 * An SMT node representing a unary bit-vector operation.
 * Only {@link BitVecOp#NEG} (two's-complement negation) and {@link BitVecOp#NOT}
 * (bitwise complement) are valid unary operations.
 * @author Jakub Wolek
 */
public record BitVecUnOp(SmtNode operand, BitVecOp op) implements SmtNode {

    public BitVecUnOp {
        if (op != BitVecOp.NEG && op != BitVecOp.NOT)
            throw new IllegalArgumentException(
                    "BvUnOp only supports NEG and NOT, got: " + op);
    }

    @Override
    public BitVecExpr toZ3(Context ctx) {
        BitVecExpr bv = (BitVecExpr) operand.toZ3(ctx);
        return switch (op) {
            case NEG -> ctx.mkBVNeg(bv);   // two's-complement negation
            case NOT -> ctx.mkBVNot(bv);   // bitwise complement
            default  -> throw new IllegalStateException("unreachable");
        };
    }

    @Override public VerificationStrategy strategy() { return VerificationStrategy.FLAT_BV; }

    @Override
    public List<SmtNode> children() {
        return List.of(operand);
    }
}