package org.glavo.japp.launcher.condition;

import java.util.Iterator;
import java.util.List;

public class OrCondition implements Condition {
    private final List<Condition> conditions;

    public OrCondition(List<Condition> conditions) {
        assert conditions.size() >= 2;
        this.conditions = conditions;
    }

    @Override
    public boolean test(JAppRuntimeContext context) {
        for (Condition condition : conditions) {
            if (condition.test(context)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        Iterator<Condition> it = conditions.iterator();
        res.append("or(");
        res.append(it.next());
        while (it.hasNext()) {
            res.append(", ").append(it.next());
        }
        res.append(')');
        return res.toString();
    }
}
