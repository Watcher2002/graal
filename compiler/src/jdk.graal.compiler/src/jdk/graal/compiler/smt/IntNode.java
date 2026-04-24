package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import jdk.graal.compiler.core.common.type.IntegerStamp;

import java.util.List;

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
        var bitVec = toZ3(ctx);

        long minValue = stamp.lowerBound();
        long maxValue = stamp.upperBound();
        long mustBeSet = stamp.mustBeSet();
        long mustBeZero = ~stamp.mayBeSet();
        boolean canBeZero = stamp.contains(0);

        BitVecExpr minValueExpr = ctx.mkBV(minValue, bitWidth);
        BitVecExpr maxValueExpr = ctx.mkBV(maxValue, bitWidth);
        BoolExpr withinBounds = ctx.mkAnd(
                ctx.mkBVSLE(minValueExpr, bitVec),
                ctx.mkBVSLE(bitVec, maxValueExpr)
        );

        BitVecExpr mustBeSetExpr = ctx.mkBV(mustBeSet, bitWidth);
        BoolExpr mustBeSetConstraint = ctx.mkEq(
                ctx.mkBVAND(bitVec, mustBeSetExpr),
                mustBeSetExpr
        );

        BitVecExpr mustBeZeroExpr = ctx.mkBV(mustBeZero, bitWidth);
        BoolExpr mustBeZeroConstraint = ctx.mkEq(
                ctx.mkBVAND(bitVec, mustBeZeroExpr),
                ctx.mkBV(0, bitWidth)
        );

        BoolExpr nonZeroConstraint = canBeZero
                ? ctx.mkTrue()
                : ctx.mkNot(ctx.mkEq(bitVec, ctx.mkBV(0, bitWidth)));

        BoolExpr signednessConstraint = signed
                ? ctx.mkTrue()
                : ctx.mkAnd(ctx.mkBVUGE(ctx.mkBV(0, bitWidth), bitVec));

        BoolExpr allConstraints = ctx.mkAnd(withinBounds, mustBeSetConstraint, mustBeZeroConstraint, nonZeroConstraint, signednessConstraint);
        return List.of(allConstraints);
    }
}