package jdk.graal.compiler.smt;

import com.microsoft.z3.Context;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FPRMExpr;

import java.util.List;

/**
 * An SMT node representing a unary floating-point operation.
 * Accepts {@link FpOp#FNEG}, {@link FpOp#FABS}, and {@link FpOp#FSQRT};
 * binary operations are rejected.
 * @author Jakub Wolek
 */
public record FpUnOp(SmtNode operand, FpOp op) implements SmtNode {

    public FpUnOp {
        if (op != FpOp.FNEG && op != FpOp.FABS && op != FpOp.FSQRT)
            throw new IllegalArgumentException("FpUnOp: not a unary op: " + op);
    }

    @Override
    public FPExpr toZ3(Context ctx) {
        FPExpr fp  = (FPExpr) operand.toZ3(ctx);
        FPRMExpr r = ctx.mkFPRNE();
        return switch (op) {
            // FNEG and FABS don't use a rounding mode in IEEE 754 —
            // Z3 still takes one for API uniformity, we pass RNE.
            case FNEG  -> ctx.mkFPNeg(fp);
            case FABS  -> ctx.mkFPAbs(fp);
            case FSQRT -> ctx.mkFPSqrt(r, fp);
            default    -> throw new IllegalStateException("unreachable");
        };
    }

    @Override public VerificationStrategy strategy() { return VerificationStrategy.MIXED_BVFP; }

    @Override
    public List<SmtNode> children() {
        return List.of(operand);
    }
}
