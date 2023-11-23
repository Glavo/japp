package org.glavo.japp.launcher.platform;

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

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
