package jdk.graal.compiler.nodes;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;

public class SMTUtils {
    public static BitVecExpr negateBV(Context ctx, BitVecExpr expr) {
        return ctx.mkBVAdd(expr, ctx.mkBV(1, expr.getSortSize()));
    }
}
