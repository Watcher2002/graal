package jdk.graal.compiler.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Sealed interface defining the common contract for all SMT AST nodes.
 * Each node can compile itself to a Z3 {@link com.microsoft.z3.Expr}, report its preferred
 * {@link VerificationStrategy}, enumerate its child nodes, and emit auxiliary SMT assumptions.
 * @author Jakub Wolek
 */
public sealed interface SmtNode
        permits BitVecBinOp, BitVecCmp, BitVecExtract, BitVecSignExt, BitVecUnOp, BitVecZeroExt, BoolBinOp, BoolNode, BoolUnOp, FloatNode, FpBinOp, FpUnOp, ITENode, IntNode, StampedNode, SymVar {

    /**
     * Compile this node into a Z3 Expr within the given context.
     */
    Expr<?> toZ3(Context ctx);

    /**
     * Which verification strategy should be used when this node is the root.
     */
    VerificationStrategy strategy();

    default List<SmtNode> children() {
        return List.of();
    }

    default List<BoolExpr> assumptions(Context ctx) {
        var result = new ArrayList<BoolExpr>();

        for (var child : children()) {
            result.addAll(child.assumptions(ctx));
        };

        return result;
    }
}