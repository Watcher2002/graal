package jdk.graal.compiler.nodes;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.FPNum;

import java.util.List;

sealed public interface SmtRepresentation {
    record IntegerRepresentation(BitVecExpr value) implements SmtRepresentation {}
}
