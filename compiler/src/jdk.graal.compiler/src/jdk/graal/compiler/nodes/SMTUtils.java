package jdk.graal.compiler.nodes;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FPSort;
import jdk.graal.compiler.debug.DebugOptions;
import jdk.graal.compiler.graph.Graph;
import org.graalvm.collections.Pair;

public class SMTUtils {
    public static boolean breakCanonicalization(String name, Graph graph){
        for (String nodeName : DebugOptions.InjectCanonicalizationFault.getValue(graph.getOptions()).split(",")) {
            if (nodeName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static Pair<BitVecExpr, BitVecExpr> normalizeBitVecSortSize(Context ctx, BitVecExpr expr1, BitVecExpr expr2) {
        var size1 = expr1.getSortSize();
        var size2 = expr2.getSortSize();

        if (size1 > size2) {
            return Pair.create(expr1, ctx.mkZeroExt(size1 - size2, expr2));
        } else if (size1 < size2) {
            return Pair.create(ctx.mkZeroExt(size2 - size1, expr1), expr2);
        }

        return Pair.create(expr1, expr2);
    }

    // TODO JAKUB ASK If you can get different sized fp or bv
    public static FPExpr BV2FP(Context ctx, BitVecExpr bvExpr, FPExpr fpExpr) {
        int diff = fpExpr.getEBits() + fpExpr.getSBits() - bvExpr.getSortSize();
        BitVecExpr bv = bvExpr;
        if (diff > 0) {
            bv = ctx.mkZeroExt(diff, bvExpr);
        }

        return ctx.mkFPToFP(bv, fpExpr.getSort());
    }

    public static FPExpr BV2FP(Context ctx, BitVecExpr bvExpr) {
        return ctx.mkFPToFP(bvExpr, ctx.mkFPSort32());
    }

    public static FPSort getFPSort(Context ctx, int bitSize) {
        return (bitSize == 32) ? ctx.mkFPSort32() : ctx.mkFPSort64();
    }
}
