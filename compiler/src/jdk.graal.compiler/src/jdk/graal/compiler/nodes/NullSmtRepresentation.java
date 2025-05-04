package jdk.graal.compiler.nodes;

import com.microsoft.z3.BoolExpr;

public class NullSmtRepresentation extends SmtRepresentation<BoolExpr>{
    public NullSmtRepresentation() {
        super(null);
    }
}
