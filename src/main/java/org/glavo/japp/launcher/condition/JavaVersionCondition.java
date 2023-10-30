package org.glavo.japp.launcher.condition;

import org.glavo.japp.thirdparty.json.JSONObject;

public final class JavaVersionCondition implements Condition {

    public static final String TYPE = "JavaVersion";

    public static JavaVersionCondition fromJson(JSONObject obj) {
        return new JavaVersionCondition(obj.getInt("Minimum"));
    }

    private final int minimum;

    public JavaVersionCondition(int minimum) {
        if (minimum < 7) {
            throw new IllegalArgumentException();
        }

        this.minimum = minimum;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("Type", TYPE);
        obj.put("Minimum", minimum);
        return obj;
    }

    @Override
    public boolean test(JAppRuntimeContext context) {
        return context.getRelease() >= minimum;
    }
}
