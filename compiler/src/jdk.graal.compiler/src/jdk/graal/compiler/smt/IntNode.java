package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import jdk.graal.compiler.core.common.type.IntegerStamp;

import java.util.List;

/**
 * An SMT node representing a named integer variable backed by a Z3 bit vector.
 * Value-range and bit-mask constraints derived from the {@link IntegerStamp} are emitted
 * as SMT assumptions consumed during verification.
 * @author Jakub Wolek
 */
public record IntNode(String name, int bitWidth, IntegerStamp stamp, boolean signed) implements SmtNode {

    @Override
    public BitVecExpr toZ3(Context ctx) {
        return ctx.mkBVConst(name, bitWidth);
    }

    @Override
    public VerificationStrategy strategy() {
        return VerificationStrategy.FLAT_BV;
    }

    @Override
    public List<BoolExpr> assumptions(Context ctx) {
        var assumptions = SmtNode.super.assumptions(ctx);
        var bitVec = toZ3(ctx);

        if (!signed) {
            long minValue = stamp.lowerBound();
            long maxValue = stamp.upperBound();

            BitVecExpr minValueExpr = ctx.mkBV(minValue, bitWidth);
            BitVecExpr maxValueExpr = ctx.mkBV(maxValue, bitWidth);
            BoolExpr withinBounds = ctx.mkAnd(
                    ctx.mkBVULE(minValueExpr, bitVec),
                    ctx.mkBVULE(bitVec, maxValueExpr)
            );

            assumptions.add(withinBounds);
        }

        return assumptions;
    }
}