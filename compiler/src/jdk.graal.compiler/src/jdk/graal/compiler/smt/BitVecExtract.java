package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;

import java.util.List;

public record BitVecExtract(SmtNode operand, int high, int low) implements SmtNode {

    public BitVecExtract {
        if (high < low)
            throw new IllegalArgumentException(
                    "BvExtract: high (" + high + ") must be >= low (" + low + ")");
        if (low < 0)
            throw new IllegalArgumentException("BvExtract: low must be >= 0");
    }

    @Override
    public BitVecExpr toZ3(Context ctx) {
        BitVecExpr bv = (BitVecExpr) operand.toZ3(ctx);
        return ctx.mkExtract(high, low, bv);
    }

    @Override public VerificationStrategy strategy() { return VerificationStrategy.FLAT_BV; }

    @Override
    public List<SmtNode> children() {
        return List.of(operand);
    }
}