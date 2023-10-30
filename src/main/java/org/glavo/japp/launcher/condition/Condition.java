package org.glavo.japp.launcher.condition;

import org.glavo.japp.thirdparty.json.JSONObject;

import java.util.function.Predicate;

public interface Condition extends Predicate<JAppRuntimeContext> {
    static Condition fromJson(JSONObject obj) {
        String type = obj.getString("Type");

        switch (type) {
            case AndCondition.TYPE:
                return AndCondition.fromJson(obj);
            case OrCondition.TYPE:
                return OrCondition.fromJson(obj);
            case JavaVersionCondition.TYPE:
                return JavaVersionCondition.fromJson(obj);
            default:
                throw new IllegalArgumentException("Unknown Type: " + type);
        }
    }

    String type();

    JSONObject toJson();

    boolean test(JAppRuntimeContext context);
}
