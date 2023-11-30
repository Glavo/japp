package org.glavo.japp.boot;

import java.util.LinkedHashMap;

public final class JAppResourceGroup extends LinkedHashMap<String, JAppResource> {

    public static final byte MAGIC_NUMBER = (byte) 0xeb;
    public static final int HEADER_LENGTH = 24; // 1 + 1 + 2 + 4 + 4 + 4 + 8

    private String name;

    public JAppResourceGroup() {
    }

    public void initName(String name) {
        if (this.name != null) {
            throw new IllegalStateException();
        }

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }

}
