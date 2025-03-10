package jdk.graal.compiler.core.test;

import jdk.graal.compiler.nodes.StructuredGraph;
import jdk.graal.compiler.nodes.calc.AbsNode;
import jdk.graal.compiler.nodes.calc.NegateNode;
import org.junit.Assert;
import org.junit.Test;

public class AbsIntegerCanonicalizationTest extends GraalCompilerTest {
    public static int absReference(int x) {
        return Math.abs(x);
    }

    public static int absAbs(int x) {
        return Math.abs(Math.abs(x));
    }

    public static int absNegate(int x) {
        return Math.abs(-x);
    }

//    @Test
//    public void testAbsNegate() {
//        StructuredGraph graph = parseEager("absNegate", StructuredGraph.AllowAssumptions.YES);
//        createInliningPhase().apply(graph, getDefaultHighTierContext());
//        createCanonicalizerPhase().apply(graph, getProviders());
//
//        StructuredGraph referenceGraph = parseEager("absReference", StructuredGraph.AllowAssumptions.YES);
//        assertEquals(referenceGraph, graph);
//        Assert.assertEquals(0, graph.getNodes().filter(NegateNode.class).count());
//
//        testAgainstExpected(graph.method(), new GraalCompilerTest.Result(absNegate(-Integer.MAX_VALUE), null), (Object) null, Integer.MAX_VALUE);
//        testAgainstExpected(graph.method(), new GraalCompilerTest.Result(absNegate(0), null), (Object) null, 0);
//        testAgainstExpected(graph.method(), new GraalCompilerTest.Result(absNegate(Integer.MAX_VALUE), null), (Object) null, Integer.MAX_VALUE);
//    }

    public static int leftShift(int x) {
        return x * 2;
    }

    @Test
    public void testLeftShift() {
        StructuredGraph graph = parseEager("leftShift", StructuredGraph.AllowAssumptions.YES);
        createInliningPhase().apply(graph, getDefaultHighTierContext());
        createCanonicalizerPhase().apply(graph, getProviders());

        testAgainstExpected(graph.method(), new GraalCompilerTest.Result(leftShift(1), null), (Object) null, 2);
    }

//    @Test
//    public void testAbsAbs() {
//        StructuredGraph graph = parseEager("absAbs", StructuredGraph.AllowAssumptions.YES);
//        createInliningPhase().apply(graph, getDefaultHighTierContext());
//        createCanonicalizerPhase().apply(graph, getProviders());
//
//        StructuredGraph referenceGraph = parseEager("absReference", StructuredGraph.AllowAssumptions.YES);
//        assertEquals(referenceGraph, graph);
//        Assert.assertEquals(1, graph.getNodes().filter(AbsNode.class).count());
//
//        testAgainstExpected(graph.method(), new GraalCompilerTest.Result(absAbs(-Integer.MAX_VALUE), null), (Object) null, Integer.MAX_VALUE);
//        testAgainstExpected(graph.method(), new GraalCompilerTest.Result(absAbs(0), null), (Object) null, 0);
//        testAgainstExpected(graph.method(), new GraalCompilerTest.Result(absAbs(Integer.MAX_VALUE), null), (Object) null, Integer.MAX_VALUE);
//    }
}