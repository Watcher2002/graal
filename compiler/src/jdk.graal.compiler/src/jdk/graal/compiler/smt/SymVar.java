package jdk.graal.compiler.smt;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import java.util.List;

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
}
