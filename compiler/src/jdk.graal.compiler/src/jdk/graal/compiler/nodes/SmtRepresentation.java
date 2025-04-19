package jdk.graal.compiler.nodes;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.FPExpr;
import com.microsoft.z3.FPNum;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import org.graalvm.collections.Pair;

import java.util.ArrayList;
import java.util.List;

sealed public interface SmtRepresentation {
    record IntegerRepresentation(BitVecExpr value) implements SmtRepresentation {
        public static List<BitVecExpr> bitVectors = new ArrayList<>(){};
    }

    record FloatRepresentation(FPExpr value) implements SmtRepresentation {
        public static List<FPExpr> floats = new ArrayList<>(){};
    }

    record UnknownRepresentation() implements SmtRepresentation {
    }
}
