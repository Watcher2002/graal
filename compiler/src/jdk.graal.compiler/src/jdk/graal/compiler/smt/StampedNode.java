package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPExpr;
import jdk.graal.compiler.core.common.type.FloatStamp;
import jdk.graal.compiler.core.common.type.IntegerStamp;
import jdk.graal.compiler.core.common.type.Stamp;
import jdk.graal.compiler.vector.nodes.simd.LogicValueStamp;
import jdk.vm.ci.meta.JavaKind;

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
        } else if (stamp instanceof FloatStamp fpStamp && representation instanceof FPExpr fpExpr) {
            var maxValue = fpStamp.upperBound();
            var minValue = fpStamp.lowerBound();
            var sort = fpStamp.getStackKind() == JavaKind.Float ? ctx.mkFPSort32() : ctx.mkFPSort64();

            FPExpr minValueExpr = ctx.mkFP(minValue, sort);
            FPExpr maxValueExpr = ctx.mkFP(maxValue, sort);
            BoolExpr bounds = ctx.mkAnd(
                    ctx.mkFPLEq(fpExpr, maxValueExpr),
                    ctx.mkFPLEq(minValueExpr, fpExpr)
            );

            BoolExpr canBeNaN = fpStamp.isNonNaN()
                    ? ctx.mkNot(ctx.mkFPIsNaN(fpExpr))
                    : ctx.mkTrue();

            BoolExpr allConstraints = ctx.mkAnd(bounds, canBeNaN);
            assumptions.add(allConstraints);
        } else if (stamp instanceof LogicValueStamp logicStamp && representation instanceof BoolExpr boolExpr) {
            if (logicStamp.equals(LogicValueStamp.TRUE)) {
                assumptions.add(boolExpr);
            } else if (logicStamp.equals(LogicValueStamp.FALSE)) {
                assumptions.add(ctx.mkNot(boolExpr));
            }
        }

        return assumptions;
    }
}
