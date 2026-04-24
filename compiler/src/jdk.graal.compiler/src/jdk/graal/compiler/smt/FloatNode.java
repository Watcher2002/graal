package jdk.graal.compiler.smt;

import com.microsoft.z3.Context;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FPSort;

public record FloatNode(String name, FloatKind kind) implements SmtNode {
    @Override
    public FPExpr toZ3(Context ctx) {
        FPSort sort = kind == FloatKind.F32
                ? ctx.mkFPSort32()
                : ctx.mkFPSort64();
        return (FPExpr) ctx.mkConst(name, sort);
    }

    @Override
    public VerificationStrategy strategy() {
        return VerificationStrategy.MIXED_BVFP;
    }

    public enum FloatKind {F32, F64}
}