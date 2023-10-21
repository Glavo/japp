package org.glavo.japp.condition;

import org.glavo.japp.TODO;
import org.glavo.japp.thirdparty.json.JSONObject;

public interface Condition {
    static Condition fromJson(String condition) {
        throw new TODO();
    }

    String type();

    JSONObject toJson();

    boolean test(ConditionalHandler handler);
}
