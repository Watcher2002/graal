package jdk.graal.compiler.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import java.util.List;

public record ITENode(SmtNode cond, SmtNode thn, SmtNode els) implements SmtNode {
    @Override
    public Expr<?> toZ3(Context ctx) {
        BoolExpr c = (BoolExpr) cond.toZ3(ctx);
        Expr<?> t = thn.toZ3(ctx);
        Expr<?> e = els.toZ3(ctx);
        return ctx.mkITE(c, t, e);
    }

    @Override
    public VerificationStrategy strategy() {
        return VerificationStrategy.FLAT_BV;
    }

    @Override
    public List<SmtNode> children() {
        return List.of(cond, thn, els);
    }
}