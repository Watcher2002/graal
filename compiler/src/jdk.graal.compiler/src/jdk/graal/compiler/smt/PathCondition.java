package jdk.graal.compiler.smt;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

import java.util.ArrayDeque;
import java.util.Deque;

public final class PathCondition {
    private final Deque<BoolExpr> stack = new ArrayDeque<>();

    public void push(BoolExpr guard) {
        stack.push(guard);
    }

    public void pop() {
        stack.pop();
    }

    public BoolExpr conjunct(Context ctx) {
        BoolExpr[] arr = stack.toArray(new BoolExpr[0]);
        return arr.length == 0 ? ctx.mkTrue() : ctx.mkAnd(arr);
    }
}
