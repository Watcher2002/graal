package jdk.graal.compiler.nodes;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import org.graalvm.collections.Pair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class SmtRepresentation<T extends Expr<?>> {
    protected Context ctx;
    protected List<BoolExpr> constantConstraints = new ArrayList<>();

    protected T expression;

    public SmtRepresentation(T expression) {
        this.expression = expression;
    }

    public SmtRepresentation(T expression, Context ctx) {
        this.expression = expression;
        this.ctx = ctx;
    }

    public SmtRepresentation(T expression, Context ctx, BoolExpr constantConstraint) {
        this(expression, ctx);
        this.constantConstraints.add(constantConstraint);
    }

    public SmtRepresentation(T expression, Context ctx, SmtRepresentation<?>... representations) {
        this(expression, ctx);
        Arrays.stream(representations).forEach(representation -> constantConstraints.addAll(representation.constantConstraints));
    }

    public T getExpression() {
        return expression;
    }

    public Pair<Status, Pair<Model, String>> compare(SmtRepresentation<?> other) {
        if (this instanceof UnknownSmtRepresentation || other instanceof UnknownSmtRepresentation) {
            return null;
        }

        if (this instanceof NullSmtRepresentation && other instanceof NullSmtRepresentation) {
            return null;
        }

        if (this instanceof NullSmtRepresentation || other instanceof NullSmtRepresentation ) {
            throw new SmtException("The values for the return node do not have the same result.");
        }

        var solver = ctx.mkSolver();

        constantConstraints.forEach(solver::add);
        other.constantConstraints.forEach(solver::add);

        var eq = ctx.mkEq(this.expression, other.expression);
        solver.add(ctx.mkNot(eq));

        var constraints = solver.toString();

        try {
            FileWriter fw = new FileWriter("smth.log", true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(constraints);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var status = solver.check();
        if (status == Status.SATISFIABLE) {
            var model = solver.getModel();
            return Pair.create(status, Pair.create(model, constraints));
        }

        return Pair.create(status, null);
    }
}
