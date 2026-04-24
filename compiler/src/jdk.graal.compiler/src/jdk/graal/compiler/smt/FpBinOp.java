package jdk.graal.compiler.smt;

import com.microsoft.z3.Context;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FPRMExpr;

import java.util.List;

public record FpBinOp(SmtNode left, SmtNode right, FpOp op) implements SmtNode {

    public FpBinOp {
        if (op == FpOp.FNEG || op == FpOp.FABS || op == FpOp.FSQRT)
            throw new IllegalArgumentException("FpBinOp: not a binary op: " + op);
    }

    @Override
    public FPExpr toZ3(Context ctx) {
        FPExpr l   = (FPExpr) left.toZ3(ctx);
        FPExpr r   = (FPExpr) right.toZ3(ctx);
        FPRMExpr m = ctx.mkFPRNE();
        return switch (op) {
            case FADD -> ctx.mkFPAdd(m, l, r);
            case FSUB -> ctx.mkFPSub(m, l, r);
            case FMUL -> ctx.mkFPMul(m, l, r);
            case FDIV -> ctx.mkFPDiv(m, l, r);
            default   -> throw new IllegalStateException("unreachable");
        };
    }

    @Override public VerificationStrategy strategy() { return VerificationStrategy.MIXED_BVFP; }

    @Override
    public List<SmtNode> children() {
        return List.of(left, right);
    }
}