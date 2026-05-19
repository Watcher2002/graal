package jdk.graal.compiler.smt;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Solver;
import jdk.graal.compiler.debug.TTY;

import java.util.HashMap;

/**
 * Checks that a canonicalization rewrite preserves semantics using the Z3 SMT solver.
 * Given the SMT encodings of the original and rewritten expressions, it asserts their
 * negated equivalence and queries Z3: UNSAT means the rewrite is valid, SAT returns a
 * counterexample witness.
 * @author Jakub Wolek
 */
public final class CanonicalizationChecker implements AutoCloseable {

    private final Context ctx;

    public CanonicalizationChecker() {
        var cfg = new HashMap<String, String>();
        cfg.put("model", "true");        // produce counterexample witnesses
        cfg.put("proof", "false");
        this.ctx = new Context(cfg);
    }

    public VerificationResult verify(SmtNode before, SmtNode after) {
        return verify(before, after, new PathCondition());
    }

    public VerificationResult verify(SmtNode before, SmtNode after, PathCondition pc) {
        return switch (before.strategy()) {
            case FLAT_BV, MIXED_BVFP -> verifyFlat(before, after);
            case INCREMENTAL_CF -> verifyIncremental(before, after, pc);
        };
    }

    public Context getCtx() {
        return ctx;
    }

    private VerificationResult verifyFlat(SmtNode before, SmtNode after) {
        Solver s = ctx.mkSolver();
        return verify(s, before, after);
    }

    private VerificationResult verifyIncremental(SmtNode before, SmtNode after,
                                                 PathCondition pc) {
        Solver s = ctx.mkSolver();
        s.push();
        try {
            // Assert all active guards from the control-flow walk
            s.add(pc.conjunct(ctx));
            return verify(s, before, after);
        } finally {
            s.pop();
        }
    }

    private VerificationResult verify(Solver solver, SmtNode before, SmtNode after) {
        Expr<?> b = before.toZ3(ctx);
        Expr<?> a = after.toZ3(ctx);
        before.assumptions(ctx).forEach(solver::add);
        after.assumptions(ctx).forEach(solver::add);
        solver.add(ctx.mkNot(ctx.mkEq(b, a)));
        return switch (solver.check()) {
            case UNSATISFIABLE -> new VerificationResult.Valid();
            case SATISFIABLE -> new VerificationResult.Counterexample(solver.toString(), solver.getModel());
            case UNKNOWN -> throw new VerificationException("Solver returned UNKNOWN");
        };
    }

    @Override
    public void close() {
        ctx.close();
    }
}
