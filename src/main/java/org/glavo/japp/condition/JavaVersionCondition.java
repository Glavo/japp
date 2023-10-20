package org.glavo.japp.condition;

public abstract class JavaVersionCondition implements Condition {

    public static final String TYPE = "JavaVersion";

    public static JavaVersionCondition fromJson(String value) {
        return new Simple(Integer.parseInt(value));
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
