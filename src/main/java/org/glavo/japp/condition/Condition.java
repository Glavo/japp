package org.glavo.japp.condition;

import org.glavo.japp.thirdparty.json.JSONObject;

public interface Condition {
    static Condition fromJson(JSONObject obj) {
        String type = obj.getString("Type");
        switch (type) {
            case JavaVersionCondition.TYPE:
                return JavaVersionCondition.fromJson(obj);
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}
