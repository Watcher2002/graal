package jdk.graal.compiler.nodes;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.FPSort;
import jdk.graal.compiler.core.common.type.IntegerStamp;

public class IntegerSmtRepresentation extends SmtRepresentation<BitVecExpr> {
    public IntegerSmtRepresentation(BitVecExpr expression) {
        super(expression);
    }

    public IntegerSmtRepresentation(BitVecExpr expression, BoolExpr constData) {
        super(expression, constData);
    }

    public IntegerSmtRepresentation(BitVecExpr expression, IntegerSmtRepresentation... originalRepresentations ) {
        super(expression, originalRepresentations);
    }

    public FloatSmtRepresentation toFloatSmtRepresentation(FPSort sort) {
        return new FloatSmtRepresentation(ctx.mkFPToFP(expression, sort));
    }

    public static IntegerSmtRepresentation fromLogical(boolean value) {
        return new IntegerSmtRepresentation(exprFromBool(value));
    }

    public static BitVecExpr exprFromBool(boolean value) {
        return ctx.mkBV((value) ? 1 : 0, 1);
    }

    public static IntegerSmtRepresentation fromStamp(IntegerStamp stamp, String nodeName) {
        int bitWidth = stamp.getBits();
        long minValue = stamp.lowerBound();
        long maxValue = stamp.upperBound();

        long mustBeSet = stamp.mustBeSet();

        var bitVecValue = ctx.mkBVConst(nodeName, bitWidth);

        BitVecExpr minValueExpr = ctx.mkBV(minValue, bitWidth);
        BitVecExpr maxValueExpr = ctx.mkBV(maxValue, bitWidth);
        BoolExpr withinBounds = ctx.mkAnd(
                ctx.mkBVSLE(minValueExpr, bitVecValue),
                ctx.mkBVSLE(bitVecValue, maxValueExpr)
        );

        BitVecExpr mustBeSetExpr = ctx.mkBV(mustBeSet, bitWidth);
        BoolExpr mustBeSetConstraint = ctx.mkEq(
                ctx.mkBVAND(bitVecValue, mustBeSetExpr),
                mustBeSetExpr
        );

        BoolExpr allConstraints = ctx.mkAnd(withinBounds, mustBeSetConstraint);
        return new IntegerSmtRepresentation(bitVecValue, allConstraints);
    }
}
