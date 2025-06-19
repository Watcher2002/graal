package jdk.graal.compiler.nodes;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.FPSort;
import jdk.graal.compiler.core.common.type.IntegerStamp;

public class IntegerSmtRepresentation extends SmtRepresentation<BitVecExpr> {
    public IntegerSmtRepresentation(BitVecExpr expression, Context ctx) {
        super(expression, ctx);
    }

    public IntegerSmtRepresentation(BitVecExpr expression, Context ctx, BoolExpr constData) {
        super(expression, ctx, constData);
    }

    public IntegerSmtRepresentation(BitVecExpr expression, Context ctx, IntegerSmtRepresentation... originalRepresentations ) {
        super(expression, ctx, originalRepresentations);
    }

//    public FloatSmtRepresentation toFloatSmtRepresentation(FPSort sort) {
//        return new FloatSmtRepresentation(ctx.mkFPToFP(expression, sort));
//    }

    public static IntegerSmtRepresentation fromLogical(boolean value, Context ctx) {
        return new IntegerSmtRepresentation(exprFromBool(value, ctx), ctx);
    }

    public static BitVecExpr exprFromBool(boolean value, Context ctx) {
        return ctx.mkBV((value) ? 1 : 0, 1);
    }

    public static IntegerSmtRepresentation fromStamp(IntegerStamp stamp, Context ctx, String nodeName) {
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
        return new IntegerSmtRepresentation(bitVecValue, ctx, allConstraints);
    }
}
