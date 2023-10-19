package org.glavo.japp.condition;

import org.glavo.japp.thirdparty.json.JSONObject;

public abstract class JavaVersionCondition implements Condition {

    public static final String TYPE = "JavaVersion";

    public static JavaVersionCondition fromJson(JSONObject obj) {
        return new Simple(Integer.parseInt(obj.getString("Value")));
    }

    public static final class Simple extends JavaVersionCondition {
        private final int minimalJavaVersion;

        public Simple(int minimalJavaVersion) {
            this.minimalJavaVersion = minimalJavaVersion;
        }

        public int getMinimalJavaVersion() {
            return minimalJavaVersion;
        }
    }
}
