package jdk.graal.compiler.nodes;

import com.microsoft.z3.FPExpr;
import jdk.graal.compiler.core.common.type.FloatStamp;

public class FloatSmtRepresentation extends SmtRepresentation<FPExpr>{
    public FloatSmtRepresentation(FPExpr expression) {
        super(expression);
    }

    public static FloatSmtRepresentation fromStamp(FloatStamp stamp, String nodeName) {
        setupZ3();

        var sort = SMTUtils.getFPSort(ctx, stamp.getBits());
        FPExpr fp = (FPExpr) ctx.mkConst(nodeName, sort);

        var minValue = ctx.mkFP(stamp.lowerBound(), sort);
        var maxValue = ctx.mkFP(stamp.upperBound(), sort);

        solver.add(ctx.mkAnd(
                ctx.mkFPLEq(minValue, fp),
                ctx.mkFPLEq(fp, maxValue)
        ));

        return new FloatSmtRepresentation(fp);
    }
}
