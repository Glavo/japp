package org.glavo.japp.platform;

import java.util.Locale;

public enum LibC {
    DEFAULT, MUSL;

    public static LibC parseLibC(String value) {
        switch (value) {
            case "":
            case "default":
            case "gnu":
                return DEFAULT;
            case "musl":
                return MUSL;
            default:
                throw new IllegalArgumentException(value);
        }
    }

    private final String checkedName = this.name().toLowerCase(Locale.ROOT);

    public String getCheckedName() {
        return checkedName;
    }

    @Override
    public String toString() {
        return getCheckedName();
    }
}
