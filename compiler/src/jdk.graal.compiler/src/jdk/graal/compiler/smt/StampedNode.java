package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import jdk.graal.compiler.core.common.type.IntegerStamp;
import jdk.graal.compiler.core.common.type.Stamp;

import java.util.List;

public record StampedNode(SmtNode node, Stamp stamp) implements SmtNode {

    @Override
    public Expr<?> toZ3(Context ctx) {
        return node.toZ3(ctx);
    }

    @Override
    public VerificationStrategy strategy() {
        return node.strategy();
    }

    @Override
    public List<SmtNode> children() {
        return List.of(node);
    }

    @Override
    public List<BoolExpr> assumptions(Context ctx) {
        var assumptions = SmtNode.super.assumptions(ctx);
        var representation = toZ3(ctx);

        if (stamp instanceof IntegerStamp intStamp && representation instanceof BitVecExpr bitVec) {
            var bitWidth = intStamp.getBits();

            long minValue = intStamp.lowerBound();
            long maxValue = intStamp.upperBound();
            long mustBeSet = intStamp.mustBeSet();
            long mustBeZero = ~intStamp.mayBeSet();
            boolean canBeZero = intStamp.contains(0);

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

            BoolExpr allConstraints = ctx.mkAnd(withinBounds, mustBeSetConstraint, mustBeZeroConstraint, nonZeroConstraint);
            assumptions.add(allConstraints);
        }

        return assumptions;
    }
}
