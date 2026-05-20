package jdk.graal.compiler.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import java.util.List;

/**
 * A symbolic variable that carries a pre-built Z3 expression.
 * Used as a placeholder for nodes whose translation is not fully determined,
 * such as opaque function calls or loop phi nodes.
 * Optional {@code sources} track the IR nodes the variable represents for debugging.
 * @author Jakub Wolek
 */
public record SymVar(String debugName, Expr<?> expr, List<SmtNode> sources) implements SmtNode {

    public SymVar(String debugName, Expr<?> expr) {
        this(debugName, expr, List.of());
    }

    @Override
    public Expr<?> toZ3(Context ctx) {
        return expr;
    }

    @Override
    public VerificationStrategy strategy() {
        return VerificationStrategy.FLAT_BV;
    }

    @Override
    public List<SmtNode> children() {
        return sources;
    }

    @Override
    public List<BoolExpr> assumptions(Context ctx) {
        var assumptions = SmtNode.super.assumptions(ctx);

        assumptions.add(ctx.mkEq(ctx.mkConst(ctx.mkSymbol(debugName), expr.getSort()), toZ3(ctx)));

        return assumptions;
    }
}
