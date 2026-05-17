package jdk.graal.compiler.smt;

import jdk.graal.compiler.debug.DebugOptions;
import jdk.graal.compiler.nodes.spi.CanonicalizerTool;

import java.util.Arrays;

public class SMTUtils {
    public static boolean introduceError(String name, CanonicalizerTool tool) {
        if (tool == null) {
            return false;
        }

        return Arrays.asList(DebugOptions.BreakCanonicalization.getValue(tool.getOptions()).split(",")).contains(name);
    }
}
