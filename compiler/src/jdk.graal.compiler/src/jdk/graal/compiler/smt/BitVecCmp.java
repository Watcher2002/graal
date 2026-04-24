package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.List;

public record BitVecCmp(SmtNode left, SmtNode right,
                        CmpOp op) implements SmtNode {

    @Override
    public BoolExpr toZ3(Context ctx) {
        BitVecExpr l = (BitVecExpr) left.toZ3(ctx);
        BitVecExpr r = (BitVecExpr) right.toZ3(ctx);
        return switch (op) {
            case EQ -> ctx.mkEq(l, r);
            case NE -> ctx.mkNot(ctx.mkEq(l, r));
            case SLT -> ctx.mkBVSLT(l, r);
            case SLE -> ctx.mkBVSLE(l, r);
            case SGT -> ctx.mkBVSGT(l, r);
            case SGE -> ctx.mkBVSGE(l, r);
            case ULT -> ctx.mkBVULT(l, r);
            case ULE -> ctx.mkBVULE(l, r);
            case UGT -> ctx.mkBVUGT(l, r);
            case UGE -> ctx.mkBVUGE(l, r);
        };
    }

    @Override
    public VerificationStrategy strategy() {
        return VerificationStrategy.FLAT_BV;
    }

    @Override
    public List<SmtNode> children() {
        return List.of(left, right);
    }
}
