package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;

import java.util.List;

public record BitVecBinOp(SmtNode left, SmtNode right, BitVecOp op) implements SmtNode {

    @Override
    public BitVecExpr toZ3(Context ctx) {
        BitVecExpr l = (BitVecExpr) left.toZ3(ctx);
        BitVecExpr r = (BitVecExpr) right.toZ3(ctx);
        return switch (op) {
            case ADD -> ctx.mkBVAdd(l, r);
            case SUB -> ctx.mkBVSub(l, r);
            case MUL -> ctx.mkBVMul(l, r);
            case SDIV -> ctx.mkBVSDiv(l, r);
            case UDIV -> ctx.mkBVUDiv(l, r);
            case SREM -> ctx.mkBVSRem(l, r);
            case UREM -> ctx.mkBVURem(l, r);
            case AND -> ctx.mkBVAND(l, r);
            case OR -> ctx.mkBVOR(l, r);
            case XOR -> ctx.mkBVXOR(l, r);
            case SHL -> ctx.mkBVSHL(l, r);
            case ASHR -> ctx.mkBVASHR(l, r);
            case LSHR -> ctx.mkBVLSHR(l, r);
            // NEG and NOT are unary — must not appear here
            case NEG, NOT -> throw new IllegalArgumentException(op + " is unary — use BvUnOp");
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