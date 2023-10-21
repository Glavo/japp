package org.glavo.japp.condition;

import org.glavo.japp.JAppRuntimeContext;
import org.glavo.japp.TODO;
import org.glavo.japp.thirdparty.json.JSONObject;

import java.util.function.Predicate;

public interface Condition extends Predicate<JAppRuntimeContext> {
    static Condition fromJson(String condition) {
        throw new TODO();
    }

    String type();

    JSONObject toJson();

    boolean test(JAppRuntimeContext context);
}
