package org.glavo.japp.launcher.condition;

import java.util.Iterator;
import java.util.List;

public class AndCondition implements Condition {
    private final List<Condition> conditions;

    public AndCondition(List<Condition> conditions) {
        assert conditions.size() >= 2;
        this.conditions = conditions;
    }

    @Override
    public boolean test(JAppRuntimeContext context) {
        for (Condition condition : conditions) {
            if (!condition.test(context)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        Iterator<Condition> it = conditions.iterator();
        res.append("and(");
        res.append(it.next());
        while (it.hasNext()) {
            res.append(", ").append(it.next());
        }
        res.append(')');
        return res.toString();
    }
}
