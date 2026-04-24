package jdk.graal.compiler.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.List;

public record BoolUnOp(SmtNode operand, BoolOp op) implements SmtNode {

    public BoolUnOp {
        if (op != BoolOp.NOT)
            throw new IllegalArgumentException("BoolUnOp only supports NOT, got: " + op);
    }

    @Override
    public BoolExpr toZ3(Context ctx) {
        BoolExpr b = (BoolExpr) operand.toZ3(ctx);
        return ctx.mkNot(b);
    }

    @Override public VerificationStrategy strategy() { return VerificationStrategy.FLAT_BV; }

    @Override
    public List<SmtNode> children() {
        return List.of(operand);
    }
}