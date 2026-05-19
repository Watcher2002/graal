package jdk.graal.compiler.nodes.test;

import static org.junit.Assert.assertNotSame;

import jdk.graal.compiler.core.common.type.IntegerStamp;
import jdk.graal.compiler.core.common.type.StampPair;
import jdk.graal.compiler.nodes.ConstantNode;
import jdk.graal.compiler.core.test.GraalCompilerTest;
import jdk.graal.compiler.debug.DebugContext;
import jdk.graal.compiler.debug.DebugOptions;
import jdk.graal.compiler.nodes.ParameterNode;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.StructuredGraph.AllowAssumptions;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.nodes.calc.AddNode;
import jdk.graal.compiler.nodes.calc.LeftShiftNode;
import jdk.graal.compiler.nodes.calc.MulNode;
import jdk.graal.compiler.nodes.calc.NegateNode;
import jdk.graal.compiler.nodes.calc.NotNode;
import jdk.graal.compiler.nodes.calc.SignExtendNode;
import jdk.graal.compiler.nodes.calc.SubNode;
import jdk.graal.compiler.nodes.calc.UnsignedRightShiftNode;
import jdk.graal.compiler.nodes.calc.XorNode;
import jdk.graal.compiler.nodes.spi.CanonicalizerTool;
import jdk.graal.compiler.nodes.spi.CoreProviders;
import jdk.graal.compiler.nodes.spi.CoreProvidersDelegate;
import jdk.graal.compiler.options.OptionValues;
import jdk.graal.compiler.smt.SmtCanonicalVerifier;
import jdk.vm.ci.meta.Assumptions;

import org.junit.Test;

/**
 * Verifies that the SMT canonicalization checker detects each intentionally injected bug in
 * arithmetic node {@code canonical()} methods.
 * <p>
 * Each test:
 * <ol>
 *   <li>Constructs the "before" node directly (bypassing BytecodeParser optimizations).</li>
 *   <li>Calls {@code node.canonical(tool)} with a {@link CanonicalizerTool} that has
 *       {@link DebugOptions#BreakCanonicalization} set to the relevant node name, so that the
 *       intentional error fires and produces a semantically wrong "after" node.</li>
 *   <li>Asserts that {@link SmtCanonicalVerifier#verifyCanonicalization} returns {@code false},
 *       meaning Z3 found a counterexample — the bug was caught.</li>
 * </ol>
 * @author Jakub Wolek
 */
public class SmtCanonicalizationErrorTest extends GraalCompilerTest {

    private static final IntegerStamp INT32 = IntegerStamp.create(32, Integer.MIN_VALUE, Integer.MAX_VALUE);

    private static ParameterNode mkInt32Param(int index) {
        return new ParameterNode(index, StampPair.createSingle(INT32));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Creates an {@link OptionValues} that has {@link DebugOptions#BreakCanonicalization} set to
     * {@code nodeName}, so that {@code SMTUtils.introduceError(nodeName, tool)} returns true.
     */
    private OptionValues errorOptions(String nodeName) {
        return new OptionValues(getInitialOptions(), DebugOptions.BreakCanonicalization, nodeName);
    }

    private static final class TestCanonicalizerTool extends CoreProvidersDelegate implements CanonicalizerTool {
        private final OptionValues options;

        TestCanonicalizerTool(CoreProviders providers, OptionValues options) {
            super(providers);
            this.options = options;
        }

        @Override public Assumptions getAssumptions()              { return null; }
        @Override public boolean canonicalizeReads()               { return false; }
        @Override public boolean allUsagesAvailable()              { return true; }
        @Override public Integer smallestCompareWidth()            { return null; }
        @Override public boolean divisionOverflowIsJVMSCompliant() { return true; }
        @Override public OptionValues getOptions()                 { return options; }
    }

    /**
     * Creates a {@link CanonicalizerTool} backed by the default high-tier providers that uses the
     * supplied {@code options} (typically with a break-canonicalization option set).
     */
    private CanonicalizerTool mkTool(OptionValues options) {
        return new TestCanonicalizerTool(getDefaultHighTierContext(), options);
    }

    /**
     * XorNode.canonical() folds {@code x ^ ~x} to {@code -1}.
     * The injected error returns {@code 0} instead.
     */
    @Test
    public void testXorSelfNegationError() {
        ParameterNode x = mkInt32Param(0);
        XorNode before = new XorNode(x, new NotNode(x));

        CanonicalizerTool tool = mkTool(errorOptions("Xor"));
        ValueNode after = before.canonical(tool);

        assertNotSame("canonical() must return a different node (error should have fired)", before, after);
        assertFalse("XorNode error: x^~x → 0 must be detected as wrong by SMT",
                SmtCanonicalVerifier.verifyCanonicalization(before, after, getDebugContext(), getDefaultHighTierContext()));
    }

    /**
     * SubNode.canonical() rewrites {@code x − c} to {@code x + (−c)}.
     * The injected error uses {@code −c − 1} as the addend.
     */
    @Test
    public void testSubConstantError() {
        SubNode before = new SubNode(mkInt32Param(0), ConstantNode.forInt(5));

        CanonicalizerTool tool = mkTool(errorOptions("Sub"));
        ValueNode after = before.canonical(tool);

        assertNotSame("canonical() must return a different node (error should have fired)", before, after);
        assertFalse("SubNode error: x−5 → x+(−6) must be detected as wrong by SMT",
                SmtCanonicalVerifier.verifyCanonicalization(before, after, getDebugContext(), getDefaultHighTierContext()));
    }

    /**
     * MulNode.canonical() strength-reduces {@code x * 2^n} to {@code x << n}.
     * The injected error increments the shift amount by 1.
     */
    @Test
    public void testMulStrengthReduceError() {
        MulNode before = new MulNode(mkInt32Param(0), ConstantNode.forInt(4));

        CanonicalizerTool tool = mkTool(errorOptions("Mul"));
        ValueNode after = before.canonical(tool);

        assertNotSame("canonical() must return a different node (error should have fired)", before, after);
        assertFalse("MulNode error: x*4 → x<<3 must be detected as wrong by SMT",
                SmtCanonicalVerifier.verifyCanonicalization(before, after, getDebugContext(), getDefaultHighTierContext()));
    }

    /**
     * NegateNode.canonical() rewrites {@code −(a − b)} to {@code b − a}.
     * The injected error returns the original {@code a − b} instead.
     */
    @Test
    public void testNegateSubError() {
        NegateNode before = new NegateNode(new SubNode(mkInt32Param(0), mkInt32Param(1)));

        CanonicalizerTool tool = mkTool(errorOptions("Negate"));
        ValueNode after = before.canonical(tool);

        assertNotSame("canonical() must return a different node (error should have fired)", before, after);
        assertFalse("NegateNode error: −(a−b) → a−b must be detected as wrong by SMT",
                SmtCanonicalVerifier.verifyCanonicalization(before, after, getDebugContext(), getDefaultHighTierContext()));
    }

    /**
     * NotNode.canonical() rewrites {@code ~(−x)} to {@code x − 1}.
     * The injected error returns {@code x + 1} instead.
     */
    @Test
    public void testNotNegateError() {
        NotNode before = new NotNode(new NegateNode(mkInt32Param(0)));

        CanonicalizerTool tool = mkTool(errorOptions("Not"));
        ValueNode after = before.canonical(tool);

        assertNotSame("canonical() must return a different node (error should have fired)", before, after);
        assertFalse("NotNode error: ~(−x) → x+1 must be detected as wrong by SMT",
                SmtCanonicalVerifier.verifyCanonicalization(before, after, getDebugContext(), getDefaultHighTierContext()));
    }

    /**
     * LeftShiftNode.canonical() combines {@code (x << a) << b} into {@code x << (a+b)}.
     * The injected error decrements the combined shift amount by 1.
     */
    @Test
    public void testLeftShiftCombineError() {
        ParameterNode x = mkInt32Param(0);
        // Outer LeftShiftNode: (x << 2) << 3
        LeftShiftNode before = new LeftShiftNode(new LeftShiftNode(x, ConstantNode.forInt(2)), ConstantNode.forInt(3));

        CanonicalizerTool tool = mkTool(errorOptions("LeftShift"));
        ValueNode after = before.canonical(tool);

        assertNotSame("canonical() must return a different node (error should have fired)", before, after);
        assertFalse("LeftShiftNode error: (x<<2)<<3 → x<<4 must be detected as wrong by SMT",
                SmtCanonicalVerifier.verifyCanonicalization(before, after, getDebugContext(), getDefaultHighTierContext()));
    }

    /**
     * UnsignedRightShiftNode.canonical() rewrites {@code (x << a) >>> a} to {@code x & mask}.
     * The injected error uses {@code mask >>> 1} (one fewer bit set).
     */
    @Test
    public void testUnsignedRightShiftMaskError() {
        ParameterNode x = mkInt32Param(0);
        ConstantNode c16 = ConstantNode.forInt(16);
        // (x << 16) >>> 16
        UnsignedRightShiftNode before = new UnsignedRightShiftNode(new LeftShiftNode(x, c16), c16);

        CanonicalizerTool tool = mkTool(errorOptions("UnsignedRightShift"));
        ValueNode after = before.canonical(tool);

        assertNotSame("canonical() must return a different node (error should have fired)", before, after);
        assertFalse("UnsignedRightShiftNode error: (x<<16)>>>16 → x&0x7FFF must be detected as wrong by SMT",
                SmtCanonicalVerifier.verifyCanonicalization(before, after, getDebugContext(), getDefaultHighTierContext()));
    }

    /**
     * AddNode.canonical() swaps {@code const + x} and re-canonicalizes, yielding an
     * improvement (e.g. {@code (x+3)+5 → x+8}).
     * The injected error discards the improvement and returns only {@code forY} (the
     * non-constant input), effectively dropping the constant added.
     * <p>
     * The outer AddNode is AddNode(const_5, AddNode(x, const_3)).
     * The correct canonical is AddNode(x, const_8); the bug returns AddNode(x, const_3).
     */
    @Test
    public void testAddDropConstantError() {
        ParameterNode x = mkInt32Param(0);
        // 5 + (x + 3): outer AddNode has const as X
        AddNode before = new AddNode(ConstantNode.forInt(5), new AddNode(x, ConstantNode.forInt(3)));

        CanonicalizerTool tool = mkTool(errorOptions("Add"));
        ValueNode after = before.canonical(tool);

        assertNotSame("canonical() must return a different node (error should have fired)", before, after);
        assertFalse("AddNode error: 5+(x+3) → x+3 must be detected as wrong by SMT",
                SmtCanonicalVerifier.verifyCanonicalization(before, after, getDebugContext(), getDefaultHighTierContext()));
    }

    /**
     * SignExtendNode.canonical() chains two sign-extensions: {@code sign_ext(sign_ext(x,8→16),16→32)}
     * simplifies to {@code sign_ext(x,8→32)}.
     * The injected error replaces the result with {@code zero_ext(sign_ext(x,8→16),16→32)},
     * which gives a different value for negative {@code x} (e.g., x=−1 gives 65535 instead of −1).
     */
    @Test
    public void testSignExtendChainedError() {
        OptionValues opts = getInitialOptions();
        DebugContext debug = getDebugContext();
        StructuredGraph graph = new StructuredGraph.Builder(opts, debug, AllowAssumptions.YES).build();

        // 8-bit parameter (signed byte range)
        IntegerStamp stamp8 = IntegerStamp.create(8, Byte.MIN_VALUE, Byte.MAX_VALUE);
        ParameterNode param = graph.addOrUnique(new ParameterNode(0, StampPair.createSingle(stamp8)));

        // Inner sign-extension 8→16 (not added to the graph; only param needs to be there)
        SignExtendNode innerSext = new SignExtendNode(param, 8, 16);

        // Outer sign-extension 16→32 — this is "before"
        SignExtendNode before = new SignExtendNode(innerSext, 16, 32);

        CanonicalizerTool tool = mkTool(errorOptions("SignExtend"));
        ValueNode after = before.canonical(tool);

        assertNotSame("canonical() must return a different node (error should have fired)", before, after);
        assertFalse("SignExtendNode error: zero_ext(sign_ext(x,8→16),16→32) must be detected as wrong by SMT",
                SmtCanonicalVerifier.verifyCanonicalization(before, after, debug, getDefaultHighTierContext()));
    }
}
