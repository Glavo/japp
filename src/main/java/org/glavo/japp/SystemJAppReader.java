package org.glavo.japp;

import java.io.IOException;
import java.nio.file.Paths;

public final class SystemJAppReader {

    public static final JAppReader READER;

    static {
        String property = System.getProperty("org.glavo.japp.file");

        JAppReader reader = null;
        if (property != null) {
            try {
                reader = new JAppReader(Paths.get(property));
            } catch (IOException ignored) {
            }
        }
        READER = reader;
    }

    private SystemJAppReader() {
    }
}
