package jdk.graal.compiler.smt;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import jdk.graal.compiler.core.common.type.Stamp;

public record SymVar(String debugName, Expr<?> expr) implements SmtNode { // TODO: Add stamp support
    @Override
    public Expr<?> toZ3(Context ctx) {
        return expr;
    }

    @Override
    public VerificationStrategy strategy() {
        return VerificationStrategy.FLAT_BV;
    }
}
