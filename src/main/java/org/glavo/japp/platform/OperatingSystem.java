package org.glavo.japp.platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public enum OperatingSystem {
    WINDOWS("Windows"),
    LINUX("Linux"),
    MACOS("macOS");

    private final String checkedName;
    private final String displayName;

    OperatingSystem(String displayName) {
        this.checkedName = this.name().toLowerCase(Locale.ROOT);
        this.displayName = displayName;
    }

    public static OperatingSystem parseOperatingSystem(String name) {
        name = name.trim().toLowerCase(Locale.ROOT);

        if (name.contains("win"))
            return WINDOWS;
        else if (name.contains("mac") || name.contains("darwin"))
            return MACOS;
        else if (name.contains("linux"))
            return LINUX;
        else
            throw new IllegalArgumentException(name);
    }

    public String getCheckedName() {
        return checkedName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Path findJavaExecutable(Path javaHome) throws IOException {
        if (this == WINDOWS) {
            return javaHome.resolve("bin/java.exe");
        } else {
            return javaHome.resolve("bin/java");
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}
