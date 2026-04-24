package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;

import java.util.List;

public record BitVecZeroExt(SmtNode operand, int extensionBits) implements SmtNode {

    public BitVecZeroExt {
        if (extensionBits <= 0)
            throw new IllegalArgumentException(
                    "BvZeroExt: extensionBits must be > 0, got: " + extensionBits);
    }

    @Override
    public BitVecExpr toZ3(Context ctx) {
        BitVecExpr bv = (BitVecExpr) operand.toZ3(ctx);
        return ctx.mkZeroExt(extensionBits, bv);
    }

    @Override public VerificationStrategy strategy() { return VerificationStrategy.FLAT_BV; }

    @Override
    public List<SmtNode> children() {
        return List.of(operand);
    }
}
