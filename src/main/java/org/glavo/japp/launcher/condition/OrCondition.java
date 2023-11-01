package org.glavo.japp.launcher.condition;

public class OrCondition implements Condition {
    private final Condition left ;
    private final Condition right;

    public OrCondition(Condition left, Condition right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean test(JAppRuntimeContext context) {
        return left.test(context) || right.test(context);
    }

    @Override
    public String toString() {
        return "or(" + left + ", " + right + ')';
    }
}
