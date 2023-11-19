package org.glavo.japp.util;

public final class ToStringBuilder {
    private final StringBuilder builder = new StringBuilder();
    private final String right;

    private boolean first = true;

    public ToStringBuilder(Object prefix, String left, String right) {
        this.right = right;
        builder.append(prefix).append(left);
    }

    public ToStringBuilder append(String name, Object value) {
        if (first) {
            first = false;
        } else {
            builder.append(", ");
        }
        builder.append(name).append('=').append(value);
        return this;
    }

    public ToStringBuilder appendIfNotNull(String name, Object value) {
        if (value != null) {
            append(name, value);
        }
        return this;
    }

    public String build() {
        return builder.append(right).toString();
    }
}
