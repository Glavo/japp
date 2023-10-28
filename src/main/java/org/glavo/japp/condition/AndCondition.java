package org.glavo.japp.condition;

import org.glavo.japp.JAppRuntimeContext;
import org.glavo.japp.thirdparty.json.JSONArray;
import org.glavo.japp.thirdparty.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class AndCondition implements Condition {

    public static final String TYPE = "And";

    public static AndCondition fromJson(JSONObject obj) {
        JSONArray array = obj.getJSONArray("Conditions");

        ArrayList<Condition> conditions = new ArrayList<>(array.length());
        for (Object condition : array) {
            conditions.add(Condition.fromJson((JSONObject) condition));
        }
        return new AndCondition(conditions);
    }

    private final List<Condition> conditions;

    public AndCondition(List<Condition> conditions) {
        if (conditions.size() < 2) {
            throw new IllegalArgumentException();
        }

        this.conditions = conditions;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("Type", TYPE);

        JSONArray array = new JSONArray();
        for (Condition condition : conditions) {
            array.put(condition.toJson());
        }
        obj.put("Conditions", array);
        return obj;
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
}
