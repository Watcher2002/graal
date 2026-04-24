package jdk.graal.compiler.phases.common;

import jdk.graal.compiler.debug.DebugContext;
import jdk.graal.compiler.debug.TTY;
import jdk.graal.compiler.graph.Node;
import jdk.graal.compiler.nodes.ConstantNode;
import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.ValueNode;
import jdk.graal.compiler.smt.CanonicalizationChecker;
import jdk.graal.compiler.smt.IRToSmtTranslator;
import jdk.graal.compiler.smt.PathCondition;
import jdk.graal.compiler.smt.SmtNode;
import jdk.graal.compiler.smt.VerificationResult;

/**
 * SMT-based semantic equivalence verifier for canonicalization rewrites.
 * <p>
 * Two entry points, matching the two SMT-checkable mutation sites in
 * CanonicalizerPhase:
 * <p>
 *   verifyCanonicalization() — Canonicalizable.canonical() path (site 2)
 *   verifyStampFold() — stamp-derived constant fold path (site 5)
 * <p>
 * The other four mutation sites are intentionally not covered:
 *   tryKillUnused — no value produced, nothing to check
 *   customSimplification — opaque callback, no (before, after) pair
 *   Simplifiable.simplify — CFG structural, mutation already committed
 *   GVN — replacing with structural duplicate, trivially sound
 * <p>
 * Activation requires BOTH:
 *   -Djdk.graal.VerifyCanonicalizationWithSMT=true
 *   -ea (JVM assertions enabled)
 * Zero overhead in production: the assert body is compiled away entirely
 * when assertions are disabled.
 */
final class SmtCanonicalVerifier {

    // Per-thread: Z3 Context and Solver are not thread-safe across threads.
    private static final ThreadLocal<CanonicalizationChecker> CHECKER =
            ThreadLocal.withInitial(CanonicalizationChecker::new);

    // Per-thread counterexample message for the assert failure string.
    private static final ThreadLocal<String> LAST_CE = new ThreadLocal<>();

    // ── Entry point 1: Canonicalizable path ──────────────────────────────────

    /**
     * Called from the assert in tryCanonicalize(), between canonical() returning
     * and performReplacement() being called. Both node and canonical are value nodes
     * in a consistent graph state.
     * <p>
     * Returns true → rewrite is sound (UNSAT) or untranslatable (optimistic)
     * Returns false → Z3 found a counterexample; assert fires with the model
     */
    static boolean verifyCanonicalization(Node before, Node canonical, DebugContext debug) {
        // canonical == node means no change — nothing to verify
        if (canonical == null || canonical == before) return true;
        // Only value-producing nodes have an SMT model
        if (!(before instanceof ValueNode vBefore))   return true;
        if (!(canonical instanceof ValueNode vAfter)) return true;

        return runCheck(vBefore, vAfter, "verifyCanonicalization", debug);
    }

    // ── Entry point 2: stamp-derived constant fold path ───────────────────────

    /**
     * Called from the assert in processNode(), before replaceAtUsages() is called
     * for a stamp-derived constant fold. stampConstant is a freshly created
     * ConstantNode — its value comes from valueNode.stamp().asConstant().
     * <p>
     * The SMT check here verifies that the constant value the stamp analysis
     * derived actually equals the node's semantics for all inputs consistent
     * with the stamp's range constraints. PiNode range constraints in the
     * PathCondition will narrow the input space appropriately.
     */
    static boolean verifyStampFold(ValueNode node, ConstantNode stampConstant, DebugContext debug) {
        return runCheck(node, stampConstant, "verifyStampFold", debug);
    }

    // ── Shared assert message accessor ────────────────────────────────────────

    /**
     * Evaluated lazily as the assert failure message — only when verify returned false.
     */
    static String counterexampleMessage() {
        String msg = LAST_CE.get();
        return msg != null ? msg : "[SMT-CANON] counterexample unavailable";
    }

    // ── Shared implementation ─────────────────────────────────────────────────

    private static boolean runCheck(ValueNode before, ValueNode after,
                                    String site, DebugContext debug) {
        try {
            CanonicalizationChecker checker = CHECKER.get();
            PathCondition pc     = new PathCondition();
            IRToSmtTranslator tx = new IRToSmtTranslator(checker.getCtx(), pc);

            IRToSmtTranslator.TranslationResult trBefore = tx.translate(before);
            IRToSmtTranslator.TranslationResult trAfter  = tx.translate(after);

            if (trBefore instanceof IRToSmtTranslator.TranslationResult.Untranslatable(String reason)) {
                debug.log(DebugContext.VERBOSE_LEVEL,
                        "[SMT-CANON:%s] UNVERIFIED %s → %s : before: %s",
                        site, before.getClass().getSimpleName(),
                        after.getClass().getSimpleName(), reason);
                return true;
            }
            if (trAfter instanceof IRToSmtTranslator.TranslationResult.Untranslatable(String reason)) {
                debug.log(DebugContext.VERBOSE_LEVEL,
                        "[SMT-CANON:%s] UNVERIFIED %s → %s : after: %s",
                        site, before.getClass().getSimpleName(),
                        after.getClass().getSimpleName(), reason);
                return true;
            }

            SmtNode smtBefore = ((IRToSmtTranslator.TranslationResult.Ok) trBefore).node();
            SmtNode smtAfter  = ((IRToSmtTranslator.TranslationResult.Ok) trAfter).node();

            return switch (checker.verify(smtBefore, smtAfter, pc)) {
                case VerificationResult.Valid() -> {
                    debug.log(DebugContext.VERY_DETAILED_LEVEL,
                            "[SMT-CANON:%s] VERIFIED %s (id=%d) → %s (id=%d)",
                            site,
                            before.getClass().getSimpleName(), before.getId(),
                            after.getClass().getSimpleName(),  after.getId());
                    yield true;
                }
                case VerificationResult.Counterexample(var formula, var model ) -> {
                    StructuredGraph graph = before.graph();
                    String msg = buildMessage(site, before, after, graph, formula, model.toString());
                    LAST_CE.set(msg);
                    TTY.println(msg);
                    yield false;
                }
            };

        } catch (Throwable t) {
            // Never crash the compiler — log and pass through
            debug.log(DebugContext.BASIC_LEVEL,
                    "[SMT-CANON:%s] INTERNAL ERROR verifying %s → %s : %s",
                    site, before.getClass().getSimpleName(),
                    after.getClass().getSimpleName(), t.getMessage());
            return true;
        }
    }

    private static String buildMessage(String site, ValueNode before, ValueNode after,
                                       StructuredGraph graph, String formula, String model) {
        return String.format(
                "%n[SMT-CANON:%s] *** UNSOUND CANONICALIZATION ***%n" +
                        "  Method  : %s%n" +
                        "  Before  : %s (id=%d)%n" +
                        "  After   : %s (id=%d)%n" +
                        "  Formula : %s" +
                        "  Z3 model: %s%n",
                site,
                graph.method() != null
                        ? graph.method().format("%H.%n(%p)") : "<unknown>",
                before.getClass().getSimpleName(), before.getId(),
                after.getClass().getSimpleName(),  after.getId(),
                formula,
                model);
    }

    private SmtCanonicalVerifier() {}
}