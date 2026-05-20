package jdk.graal.compiler.smt;

import jdk.graal.compiler.debug.DebugOptions;
import jdk.graal.compiler.nodes.spi.CanonicalizerTool;

import java.util.Arrays;

/**
 * Utility helpers for the SMT verification infrastructure.
 * Provides {@link #introduceError} which, when the {@code SmtCheckerErrorNodes} debug option
 * is set, deliberately corrupts the canonicalized node to exercise the checker's
 * counterexample-detection path.
 * @author Jakub Wolek
 */
public class SMTUtils {
    public static boolean introduceError(String name, CanonicalizerTool tool) {
        if (tool == null) {
            return false;
        }

        return Arrays.asList(DebugOptions.BreakCanonicalization.getValue(tool.getOptions()).split(",")).contains(name);
    }
}
