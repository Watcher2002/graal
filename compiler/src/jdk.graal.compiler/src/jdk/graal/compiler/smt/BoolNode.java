package jdk.graal.compiler.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

public record BoolNode(String name) implements SmtNode {
    @Override
    public BoolExpr toZ3(Context ctx) {
        return ctx.mkBoolConst(name);
    }

    @Override
    public VerificationStrategy strategy() {
        return VerificationStrategy.FLAT_BV;
    }
}