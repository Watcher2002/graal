package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import jdk.graal.compiler.core.common.type.IntegerStamp;

import java.util.List;

public record IntNode(String name, int bitWidth, IntegerStamp stamp, boolean signed) implements SmtNode {

    @Override
    public BitVecExpr toZ3(Context ctx) {
        return ctx.mkBVConst(name, bitWidth);
    }

    @Override
    public VerificationStrategy strategy() {
        return VerificationStrategy.FLAT_BV;
    }

    @Override
    public List<BoolExpr> assumptions(Context ctx) {
        var assumptions = SmtNode.super.assumptions(ctx);
        var bitVec = toZ3(ctx);

        BoolExpr signednessConstraint = signed
                ? ctx.mkTrue()
                : ctx.mkBVSGE(bitVec, ctx.mkBV(0, bitWidth));

        assumptions.add(signednessConstraint);
        return assumptions;
    }
}