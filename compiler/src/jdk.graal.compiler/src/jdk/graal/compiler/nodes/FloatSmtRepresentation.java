package jdk.graal.compiler.nodes;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.FPExpr;
import jdk.graal.compiler.core.common.type.FloatStamp;

public class FloatSmtRepresentation extends SmtRepresentation<FPExpr> {
    public FloatSmtRepresentation(FPExpr expression) {
        super(expression);
    }

    public FloatSmtRepresentation(FPExpr expression, BoolExpr constantConstraint) {
        super(expression, constantConstraint);
    }

    public FloatSmtRepresentation(FPExpr expression, FloatSmtRepresentation... representations) {
        super(expression, representations);
    }

    public static FloatSmtRepresentation fromStamp(FloatStamp stamp, String nodeName) {
        var sort = SMTUtils.getFPSort(ctx, stamp.getBits());
        FPExpr fp = (FPExpr) ctx.mkConst(nodeName, sort);

        var minValue = ctx.mkFP(stamp.lowerBound(), sort);
        var maxValue = ctx.mkFP(stamp.upperBound(), sort);

        var expr = ctx.mkAnd(
                ctx.mkFPLEq(minValue, fp),
                ctx.mkFPLEq(fp, maxValue)
        );


        return new FloatSmtRepresentation(fp, expr);
    }
}
