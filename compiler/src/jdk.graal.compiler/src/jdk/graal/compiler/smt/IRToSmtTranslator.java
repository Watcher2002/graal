package jdk.graal.compiler.smt;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FPRMSort;
import com.microsoft.z3.FPSort;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Sort;
import jdk.graal.compiler.core.common.type.IntegerStamp;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.nodes.ConstantNode;
import jdk.graal.compiler.nodes.GuardedValueNode;
import jdk.graal.compiler.nodes.LogicNegationNode;
import jdk.graal.compiler.nodes.LogicNode;
import jdk.graal.compiler.nodes.LoopBeginNode;
import jdk.graal.compiler.nodes.NodeView;
import jdk.graal.compiler.nodes.ParameterNode;
import jdk.graal.compiler.nodes.PiNode;
import jdk.graal.compiler.nodes.ShortCircuitOrNode;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.nodes.ValuePhiNode;
import jdk.graal.compiler.nodes.calc.AbsNode;
import jdk.graal.compiler.nodes.calc.AddNode;
import jdk.graal.compiler.nodes.calc.AndNode;
import jdk.graal.compiler.nodes.calc.BinaryNode;
import jdk.graal.compiler.nodes.calc.ConditionalNode;
import jdk.graal.compiler.nodes.calc.FloatConvertNode;
import jdk.graal.compiler.nodes.calc.FloatEqualsNode;
import jdk.graal.compiler.nodes.calc.FloatLessThanNode;
import jdk.graal.compiler.nodes.calc.IntegerBelowNode;
import jdk.graal.compiler.nodes.calc.IntegerEqualsNode;
import jdk.graal.compiler.nodes.calc.IntegerLessThanNode;
import jdk.graal.compiler.nodes.calc.LeftShiftNode;
import jdk.graal.compiler.nodes.calc.MulNode;
import jdk.graal.compiler.nodes.calc.NarrowNode;
import jdk.graal.compiler.nodes.calc.NegateNode;
import jdk.graal.compiler.nodes.calc.OrNode;
import jdk.graal.compiler.nodes.calc.ReinterpretNode;
import jdk.graal.compiler.nodes.calc.RightShiftNode;
import jdk.graal.compiler.nodes.calc.SignExtendNode;
import jdk.graal.compiler.nodes.calc.SignedDivNode;
import jdk.graal.compiler.nodes.calc.SignedRemNode;
import jdk.graal.compiler.nodes.calc.SqrtNode;
import jdk.graal.compiler.nodes.calc.SubNode;
import jdk.graal.compiler.nodes.calc.UnsignedDivNode;
import jdk.graal.compiler.nodes.calc.UnsignedRemNode;
import jdk.graal.compiler.nodes.calc.UnsignedRightShiftNode;
import jdk.graal.compiler.nodes.calc.XorNode;
import jdk.graal.compiler.nodes.calc.ZeroExtendNode;
import jdk.graal.compiler.nodes.java.LoadFieldNode;
import jdk.graal.compiler.nodes.java.LoadIndexedNode;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class IRToSmtTranslator {

    private final Context ctx;
    private final PathCondition pathCondition;
    private final Map<Node, SmtNode> memo = new IdentityHashMap<>();
    private final Map<Node, Integer> nodeIds = new IdentityHashMap<>();
    private int nextNodeId = 0;
    private final Map<String, FuncDecl<?>> ufCache = new HashMap<>();

    public IRToSmtTranslator(Context ctx, PathCondition pathCondition) {
        this.ctx = ctx;
        this.pathCondition = pathCondition;
    }

    public sealed interface TranslationResult
            permits TranslationResult.Ok, TranslationResult.Untranslatable {
        record Ok(SmtNode node) implements TranslationResult {
        }

        record Untranslatable(String reason) implements TranslationResult {
        }
    }

    public TranslationResult translate(ValueNode node) {
        try {
            return new TranslationResult.Ok(translateNode(node));
        } catch (UntranslatableException e) {
            return new TranslationResult.Untranslatable(e.getMessage());
        }
    }

    private SmtNode translateNode(ValueNode node) {
        SmtNode cached = memo.get(node);
        if (cached != null) {
            return cached;
        }

        SmtNode result = switch (node) {

            // ── Constants ─────────────────────────────────────────────────────
            case ConstantNode cn -> translateConstant(cn);

            // ── Parameters ───────────────────────────────────────────────────
            case ParameterNode pn -> translateParameter(pn);

            // ── Integer / float binary arithmetic ─────────────────────────────
            case AddNode n -> translateBinaryArith(n.getX(), n.getY(), n);
            case SubNode n -> translateBinaryArith(n.getX(), n.getY(), n);
            case MulNode n -> translateBinaryArith(n.getX(), n.getY(), n);

            // ── Integer-only arithmetic ────────────────────────────────────────
            case SignedDivNode n -> bvBinOp(n.getX(), n.getY(), BitVecOp.SDIV);
            case UnsignedDivNode n -> bvBinOp(n.getX(), n.getY(), BitVecOp.UDIV);
            case SignedRemNode n -> bvBinOp(n.getX(), n.getY(), BitVecOp.SREM);
            case UnsignedRemNode n -> bvBinOp(n.getX(), n.getY(), BitVecOp.UREM);

            // ── Bitwise / shifts (integer only) ───────────────────────────────
            case AndNode n -> bvBinOp(n.getX(), n.getY(), BitVecOp.AND);
            case OrNode n -> bvBinOp(n.getX(), n.getY(), BitVecOp.OR);
            case XorNode n -> bvBinOp(n.getX(), n.getY(), BitVecOp.XOR);

            case LeftShiftNode n -> translateJavaShift(n, n.getX(), n.getY(), BitVecOp.SHL);
            case RightShiftNode n -> translateJavaShift(n, n.getX(), n.getY(), BitVecOp.ASHR);
            case UnsignedRightShiftNode n -> translateJavaShift(n, n.getX(), n.getY(), BitVecOp.LSHR);

            // ── Negation (int and float) ───────────────────────────────────────
            case NegateNode n -> translateNegate(n);

            // ── Abs (int and float) ───────────────────────────────────────────
            case AbsNode n -> translateAbs(n);

            // ── Sqrt (float only) ─────────────────────────────────────────────
            case SqrtNode n -> fpUnOp(n.getValue(), FpOp.FSQRT);

            // ── Integer comparisons (return LogicNode / Bool) ──────────────────
            case IntegerLessThanNode n -> bvCmp(n.getX(), n.getY(), CmpOp.SLT);
            case IntegerBelowNode n -> bvCmp(n.getX(), n.getY(), CmpOp.ULT);
            case IntegerEqualsNode n -> bvCmp(n.getX(), n.getY(), CmpOp.EQ);

            // ── Float comparisons ──────────────────────────────────────────────
            case FloatEqualsNode n -> fpEq(n.getX(), n.getY());
            case FloatLessThanNode n -> fpLt(n.getX(), n.getY(), n.unorderedIsTrue());

            // ── Boolean logic ──────────────────────────────────────────────────
            case LogicNegationNode n -> new BoolUnOp(translateNode(asValue(n.getValue())), BoolOp.NOT);

            // ShortCircuitOrNode has LogicNode inputs, not ValueNode inputs.
            case ShortCircuitOrNode n -> new BoolBinOp(
                    translateLogic(n.getX()),
                    translateLogic(n.getY()),
                    BoolOp.OR);

            // ── Conditional / ternary ──────────────────────────────────────────
            case ConditionalNode n -> new ITENode(
                    translateLogic(n.condition()),
                    translateNode(n.trueValue()),
                    translateNode(n.falseValue()));

            // ── Width conversions ──────────────────────────────────────────────
            case NarrowNode n -> translateNarrow(n);
            case SignExtendNode n -> translateSignExtend(n);
            case ZeroExtendNode n -> translateZeroExtend(n);

            // ── Float ↔ int conversion ─────────────────────────────────────────
            case FloatConvertNode n -> translateFloatConvert(n);

            // ── Bit reinterpretation (e.g. Float.floatToRawIntBits) ────────────
            case ReinterpretNode n -> translateReinterpret(n);

            // ── Phi ────────────────────────────────────────────────────────────
            case ValuePhiNode n -> translatePhi(n);

            // ── Type narrowing / stamp ─────────────────────────────────────────
            case PiNode n -> translatePi(n);
            case GuardedValueNode n -> translateNode(n.object());

            // ── Memory reads — opaque UF ───────────────────────────────────────
            case LoadFieldNode n -> opaqueUf(n);
            case LoadIndexedNode n -> opaqueUf(n);

            // ── Anything else ──────────────────────────────────────────────────
            default -> throw new UntranslatableException(
                    "No SMT translation for " + node.getClass().getSimpleName()
                            + " (id=" + stableNodeId(node) + ")");
        };

        SmtNode stamped = new StampedNode(result, node.stamp(NodeView.DEFAULT));
        memo.put(node, stamped);
        return stamped;
    }

    private int stableNodeId(Node node) {
        return nodeIds.computeIfAbsent(node, n -> nextNodeId++);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Dispatches AddNode/SubNode/MulNode on stamp kind.
     * In Graal, the same AST node class is used for int and float arithmetic;
     * the stamp records which kind it actually is.
     */
    private SmtNode translateBinaryArith(ValueNode l, ValueNode r, BinaryNode n) {
        JavaKind kind = n.stamp(NodeView.DEFAULT).getStackKind();
        return switch (kind) {
            case Int, Long -> {
                BitVecOp op = n instanceof AddNode ? BitVecOp.ADD
                        : n instanceof SubNode ? BitVecOp.SUB
                        : /* MulNode */           BitVecOp.MUL;
                yield bvBinOp(l, r, op);
            }
            case Float, Double -> {
                FpOp op = n instanceof AddNode ? FpOp.FADD
                        : n instanceof SubNode ? FpOp.FSUB
                        : /* MulNode */           FpOp.FMUL;
                yield fpBinOp(l, r, op);
            }
            default -> throw new UntranslatableException(
                    "Unexpected kind " + kind + " on " + n.getClass().getSimpleName());
        };
    }

    /**
     * NegateNode is a UnaryArithmeticNode — dispatches on stamp kind.
     */
    private SmtNode translateNegate(NegateNode n) {
        JavaKind kind = n.stamp(NodeView.DEFAULT).getStackKind();
        SmtNode inner = translateNode(n.getValue());
        return switch (kind) {
            case Int, Long -> new BitVecUnOp(inner, BitVecOp.NEG);
            case Float, Double -> new FpUnOp(inner, FpOp.FNEG);
            default -> throw new UntranslatableException("NegateNode: unknown kind " + kind);
        };
    }

    private SmtNode translateAbs(AbsNode n) {
        JavaKind kind = n.stamp(NodeView.DEFAULT).getStackKind();
        SmtNode x = translateNode(n.getValue());
        return switch (kind) {
            case Int, Long -> {
                int bits = kind == JavaKind.Int ? 32 : 64;
                SmtNode zero = new SymVar("0", ctx.mkBV(0, bits));
                SmtNode neg = new BitVecUnOp(x, BitVecOp.NOT);
                SmtNode lt = new BitVecCmp(x, zero, CmpOp.SLT);
                yield new ITENode(lt, neg, x);
            }
            case Float, Double -> new FpUnOp(x, FpOp.FABS);
            default -> throw new UntranslatableException("AbsNode: unknown kind " + kind);
        };
    }

    private SmtNode fpEq(ValueNode l, ValueNode r) {
        FPExpr fl = (FPExpr) translateNode(l).toZ3(ctx);
        FPExpr fr = (FPExpr) translateNode(r).toZ3(ctx);
        return new SymVar("fpeq", ctx.mkFPEq(fl, fr));
    }

    private SmtNode fpLt(ValueNode l, ValueNode r, boolean unordered) {
        FPExpr fl = (FPExpr) translateNode(l).toZ3(ctx);
        FPExpr fr = (FPExpr) translateNode(r).toZ3(ctx);
        BoolExpr lt = ctx.mkFPLt(fl, fr);
        if (unordered) {
            BoolExpr nanL = ctx.mkFPIsNaN(fl);
            BoolExpr nanR = ctx.mkFPIsNaN(fr);
            return new SymVar("fpult", ctx.mkOr(nanL, nanR, lt));
        }
        return new SymVar("fplt", lt);
    }

    /**
     * Float binary operation — all with explicit RNE rounding mode.
     */
    private SmtNode fpBinOp(ValueNode l, ValueNode r, FpOp op) {
        return new FpBinOp(translateNode(l), translateNode(r), op);
    }

    private SmtNode fpUnOp(ValueNode v, FpOp op) {
        return new FpUnOp(translateNode(v), op);
    }

    private SmtNode bvBinOp(ValueNode l, ValueNode r, BitVecOp op) {
        return new BitVecBinOp(translateNode(l), translateNode(r), op);
    }

    private SmtNode bvCmp(ValueNode l, ValueNode r, CmpOp op) {
        return new BitVecCmp(translateNode(l), translateNode(r), op);
    }

    private SmtNode translateLogic(LogicNode logic) {
        if (logic instanceof ValueNode vn) return translateNode(vn);
        throw new UntranslatableException("LogicNode is not a ValueNode: " + logic.getClass().getSimpleName());
    }

    private ValueNode asValue(Node n) {
        if (n instanceof ValueNode vn) return vn;
        throw new UntranslatableException("Expected ValueNode, got: " + n.getClass().getSimpleName());
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    private SmtNode translateConstant(ConstantNode cn) {
        JavaConstant jc = (JavaConstant) cn.getValue();
        return switch (jc.getJavaKind()) {
            case Int -> new SymVar(String.valueOf(jc.asInt()),
                    ctx.mkBV(jc.asInt(), 32));
            case Long -> new SymVar(String.valueOf(jc.asLong()),
                    ctx.mkBV(jc.asLong(), 64));
            case Float -> new SymVar(String.valueOf(jc.asFloat()),
                    ctx.mkFP(jc.asFloat(), ctx.mkFPSort32()));
            case Double -> new SymVar(String.valueOf(jc.asDouble()),
                    ctx.mkFP(jc.asDouble(), ctx.mkFPSort64()));
            case Boolean -> new SymVar(String.valueOf(jc.asInt() != 0),
                    jc.asInt() != 0 ? ctx.mkTrue() : ctx.mkFalse());
            default -> throw new UntranslatableException(
                    "Constant kind not modeled: " + jc.getJavaKind());
        };
    }

    // ── Parameters ────────────────────────────────────────────────────────────

    private SmtNode translateParameter(ParameterNode n) {
        return freshVar("p" + n.index(), n);
    }

    // ── Shift Conversion -──────────────────────────────────────────────────────

    private SmtNode translateJavaShift(ValueNode shiftNode, ValueNode xNode, ValueNode yNode, BitVecOp op) {
        SmtNode x = translateNode(xNode);
        SmtNode y = translateNode(yNode);

        BitVecExpr xBv = (BitVecExpr) x.toZ3(ctx);
        BitVecExpr yBv = (BitVecExpr) y.toZ3(ctx);

        int lhsBits = xBv.getSortSize();
        int extendedBits = (shiftNode.stamp(NodeView.DEFAULT).getStackKind() == JavaKind.Int) ? 5 : 6;

        BitVecExpr low = ctx.mkExtract(extendedBits - 1, 0, yBv);
        BitVecExpr extended = ctx.mkZeroExt(lhsBits - extendedBits, low);

        BitVecExpr shifted = switch (op) {
            case SHL -> ctx.mkBVSHL(xBv, extended);
            case ASHR -> ctx.mkBVASHR(xBv, extended);
            case LSHR -> ctx.mkBVLSHR(xBv, extended);
            default -> throw new IllegalArgumentException("not a shift op: " + op);
        };

        return new SymVar("shift_" + stableNodeId(shiftNode), shifted);
    }


    // ── Width conversions ──────────────────────────────────────────────────────

    private SmtNode translateNarrow(NarrowNode n) {
        SmtNode input = translateNode(n.getValue());
        int to = n.getResultBits();
        return new BitVecExtract(input, to - 1, 0);
    }

    private SmtNode translateSignExtend(SignExtendNode n) {
        SmtNode input = translateNode(n.getValue());
        int from = n.getInputBits(), to = n.getResultBits();
        return new BitVecSignExt(input, to - from);
    }

    private SmtNode translateZeroExtend(ZeroExtendNode n) {
        SmtNode input = translateNode(n.getValue());
        int from = n.getInputBits(), to = n.getResultBits();
        return new BitVecZeroExt(input, to - from);
    }

    // ── PiNode — stamp-range injection ────────────────────────────────────────

    private SmtNode translatePi(PiNode n) {
        SmtNode inner = translateNode(n.object());
        if (n.stamp(NodeView.DEFAULT) instanceof IntegerStamp stamp
                && inner.toZ3(ctx) instanceof BitVecExpr bv) {
            int bits = stamp.getBits();
            pathCondition.push(ctx.mkAnd(
                    ctx.mkBVSLE(ctx.mkBV(stamp.lowerBound(), bits), bv),
                    ctx.mkBVSLE(bv, ctx.mkBV(stamp.upperBound(), bits))
            ));
        }
        return inner;
    }

    // Float convert
    private SmtNode translateFloatConvert(FloatConvertNode n) {
        JavaKind inputKind = n.getValue().stamp(NodeView.DEFAULT).getStackKind();
        JavaKind outputKind = n.stamp(NodeView.DEFAULT).getStackKind();

        SmtNode input = translateNode(n.getValue());
        String name = "floatConvert_" + stableNodeId(n);

        return switch (inputKind) {
            case Float, Double -> {
                FPExpr fpExpr = (FPExpr) input.toZ3(ctx);
                Expr<FPRMSort> sort = ctx.mkFPRoundNearestTiesToEven();
                int bitVecSize = outputKind == JavaKind.Int ? 32 : 64;

                yield new SymVar(name, ctx.mkFPToBV(sort, fpExpr, bitVecSize, true));
            }

            case Int, Long -> {
                BitVecExpr bvExpr = (BitVecExpr) input.toZ3(ctx);
                var signedness = ((IntNode) input).signed();
                Expr<FPRMSort> roundingMode = ctx.mkFPRoundNearestTiesToEven();
                var sort = outputKind == JavaKind.Int ? ctx.mkFPSort32() : ctx.mkFPSort64();
                yield new SymVar(name, ctx.mkFPToFP(roundingMode, bvExpr, sort, signedness));
            }

            default -> throw new UntranslatableException("FloatConvertNode: unknown kind " + inputKind);
        };
    }

    // Reinterpret
    private SmtNode translateReinterpret(ReinterpretNode n) {
        JavaKind inputKind = n.getValue().stamp(NodeView.DEFAULT).getStackKind();
        JavaKind outputKind = n.stamp(NodeView.DEFAULT).getStackKind();

        SmtNode input = translateNode(n.getValue());
        String name = "reinterpret_bv2fp" + stableNodeId(n);

        return switch (inputKind) {
            case Float, Double -> {
                FPExpr fp = (FPExpr) input.toZ3(ctx);
                yield new SymVar(name, ctx.mkFPToIEEEBV(fp));
            }

            case Int, Long -> {
                BitVecExpr bv = (BitVecExpr) input.toZ3(ctx);
                FPSort sort = outputKind == JavaKind.Double ? ctx.mkFPSort64() : ctx.mkFPSort32();
                yield new SymVar(name, ctx.mkFPToFP(bv, sort));
            }

            default -> throw new UntranslatableException("ReinterpretNode: unknown kind " + inputKind);

        };
    }

    // ── Phi ───────────────────────────────────────────────────────────────────

    private SmtNode translatePhi(ValuePhiNode n) {
        if (n.merge() instanceof LoopBeginNode) {
            // LoopBeginNode doesn't propagate that it is guarded to be unsigned
            // to its stamp.
            return freshVar("loopPhi_" + stableNodeId(n), n, ((LoopBeginNode) n.merge()).isProtectedNonOverflowingUnsigned());
        }
        List<ValueNode> values = new ArrayList<>(n.values());
        if (values.size() == 1) return translateNode(values.getFirst());
        SmtNode result = translateNode(values.getLast());
        for (int i = values.size() - 2; i >= 0; i--) {
            BoolNode pred = new BoolNode("phi_pred_" + stableNodeId(n) + "_" + i);
            result = new ITENode(pred, translateNode(values.get(i)), result);
        }
        return result;
    }

    // ── Opaque uninterpreted function ─────────────────────────────────────────

    private SmtNode opaqueUf(ValueNode n) {
        String key = n.getClass().getSimpleName() + "_" + stableNodeId(n);
        Sort returnSort = sortFor(n);
        List<Expr<?>> argExprs = new ArrayList<>();
        List<Sort> argSorts = new ArrayList<>();
        for (Node input : n.inputs()) {
            if (input instanceof ValueNode vn) {
                try {
                    Expr<?> e = translateNode(vn).toZ3(ctx);
                    argExprs.add(e);
                    argSorts.add(e.getSort());
                } catch (UntranslatableException ignored) {
                }
            }
        }
        FuncDecl<?> fd = ufCache.computeIfAbsent(key, k ->
                ctx.mkFuncDecl(k, argSorts.toArray(new Sort[0]), returnSort));
        return new SymVar(key, fd.apply(argExprs.toArray(new Expr[0])));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private SmtNode freshVar(String name, ValueNode n) {
        return freshVar(name, n, true);
    }

    private SmtNode freshVar(String name, ValueNode n, boolean signed) {
        JavaKind kind = n.stamp(NodeView.DEFAULT).getStackKind();
        return switch (kind) {
            case Int -> new IntNode(name, 32, (IntegerStamp) n.stamp(NodeView.DEFAULT), signed);
            case Long -> new IntNode(name, 64, (IntegerStamp) n.stamp(NodeView.DEFAULT), signed);
            case Float -> new FloatNode(name, FloatNode.FloatKind.F32);
            case Double -> new FloatNode(name, FloatNode.FloatKind.F64);
            default -> throw new UntranslatableException("freshVar: unknown kind " + kind);
        };
    }

    private Sort sortFor(ValueNode n) {
        return switch (n.stamp(NodeView.DEFAULT).getStackKind()) {
            case Int -> ctx.mkBitVecSort(32);
            case Long -> ctx.mkBitVecSort(64);
            case Float -> ctx.mkFPSort32();
            case Double -> ctx.mkFPSort64();
            default -> ctx.mkBitVecSort(64);
        };
    }

    static final class UntranslatableException extends RuntimeException {
        UntranslatableException(String msg) {
            super(msg);
        }
    }
}