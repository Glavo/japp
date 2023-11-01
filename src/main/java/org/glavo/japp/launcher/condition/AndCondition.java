package org.glavo.japp.launcher.condition;

public class AndCondition implements Condition {
    private final Condition left ;
    private final Condition right;

    public AndCondition(Condition left, Condition right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean test(JAppRuntimeContext context) {
        return left.test(context) && right.test(context);
    }

    @Override
    public String toString() {
        return "and(" + left + ", " + right + ')';
    }
}
