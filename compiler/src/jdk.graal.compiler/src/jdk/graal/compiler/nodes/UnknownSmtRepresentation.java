package jdk.graal.compiler.nodes;

import com.microsoft.z3.BoolExpr;
import jdk.graal.compiler.graph.Node;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class UnknownSmtRepresentation extends SmtRepresentation<BoolExpr> {
    private static final Set<String> unrepresentableNodes = Set.of();
    public UnknownSmtRepresentation(Node node) {
        super(null);

        var nodeName = node.getClass().getSimpleName();
        if (unrepresentableNodes.contains(nodeName)) {
            return;
        }

        try (FileWriter fw = new FileWriter("smt_missed_nodes.info", true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(nodeName);
            bw.newLine();
        } catch (IOException ignored) {
        }
    }
}
