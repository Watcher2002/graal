package jdk.graal.compiler.nodes;

import com.microsoft.z3.BoolExpr;

public class UnknownSmtRepresentation extends SmtRepresentation<BoolExpr> {
    public UnknownSmtRepresentation() {
        super(null);
    }
}
