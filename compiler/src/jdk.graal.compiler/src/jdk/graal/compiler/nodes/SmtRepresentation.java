package jdk.graal.compiler.nodes;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import org.graalvm.collections.Pair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public abstract class SmtRepresentation<T extends Expr<?>> {
    protected static Context ctx;
    protected static Solver solver;

    protected T expression;

    public SmtRepresentation(T expression) {
        setupZ3();
        this.expression = expression;
    }

    public void setExpression(T expression) {
        this.expression = expression;
    }

    public T getExpression() {
        return expression;
    }

    public Context getContext() {
        return ctx;
    }

    public Pair<Status, Model> compare(SmtRepresentation<?> other) {
        if (this instanceof UnknownSmtRepresentation || other instanceof UnknownSmtRepresentation) {
            return null;
        }

        var eq = ctx.mkEq(this.expression, other.expression);
        solver.add(ctx.mkNot(eq));

        try {
            FileWriter fw = new FileWriter("smth.log", true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(solver.toString());
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        var status = solver.check();
        if (status == Status.SATISFIABLE) {
            return Pair.create(status, solver.getModel());
        }

        setupZ3();

        return Pair.create(status, null);
    }

    protected static void setupZ3() {
        if (ctx != null) {
            return;
        }
        var cfg = new HashMap<String, String>();
        cfg.put("proof", "true");
        ctx = new Context(cfg);
        solver = ctx.mkSolver();
    }
}
