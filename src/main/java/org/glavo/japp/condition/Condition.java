package org.glavo.japp.condition;

public interface Condition {
    static Condition fromJson(String condition) {
        int idx = condition.indexOf(':');
        String type = idx > 0 ? condition.substring(0, idx) : condition;
        String value = idx > 0 ? condition.substring(idx + 1) : "";
        switch (type) {
            case JavaVersionCondition.TYPE:
                return JavaVersionCondition.fromJson(value);
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}
