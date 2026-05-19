package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;

import java.util.List;

/**
 * An SMT node that sign-extends a bit vector by replicating its most significant bit.
 * {@code extensionBits} additional bits are appended at the high end; must be positive.
 * @author Jakub Wolek
 */
public record BitVecSignExt(SmtNode operand, int extensionBits) implements SmtNode {

    public BitVecSignExt {
        if (extensionBits <= 0)
            throw new IllegalArgumentException(
                    "BvSignExt: extensionBits must be > 0, got: " + extensionBits);
    }

    @Override
    public BitVecExpr toZ3(Context ctx) {
        BitVecExpr bv = (BitVecExpr) operand.toZ3(ctx);
        return ctx.mkSignExt(extensionBits, bv);
    }

    @Override public VerificationStrategy strategy() { return VerificationStrategy.FLAT_BV; }

    @Override
    public List<SmtNode> children() {
        return List.of(operand);
    }
}
