package jdk.graal.compiler.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.List;

/**
 * An SMT node representing a binary boolean operation.
 * Supports {@link BoolOp#AND}, {@link BoolOp#OR}, {@link BoolOp#XOR}, and
 * {@link BoolOp#IMPLIES}; {@link BoolOp#NOT} is rejected as it is unary.
 * @author Jakub Wolek
 */
public record BoolBinOp(SmtNode left, SmtNode right, BoolOp op) implements SmtNode {

    @Override
    public BoolExpr toZ3(Context ctx) {
        BoolExpr l = (BoolExpr) left.toZ3(ctx);
        BoolExpr r = (BoolExpr) right.toZ3(ctx);
        return switch (op) {
            case AND     -> ctx.mkAnd(l, r);
            case OR      -> ctx.mkOr(l, r);
            case XOR     -> ctx.mkXor(l, r);
            case IMPLIES -> ctx.mkImplies(l, r);
            case NOT     -> throw new IllegalArgumentException("NOT is unary — use BoolUnOp");
        };
    }

    @Override public VerificationStrategy strategy() { return VerificationStrategy.FLAT_BV; }

    @Override
    public List<SmtNode> children() {
        return List.of(left, right);
    }
}