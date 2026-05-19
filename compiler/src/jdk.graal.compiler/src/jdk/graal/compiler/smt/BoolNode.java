package jdk.graal.compiler.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

/**
 * An SMT node representing a named boolean constant.
 * The name is passed directly to the Z3 context to create a {@code BoolConst}.
 * @author Jakub Wolek
 */
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