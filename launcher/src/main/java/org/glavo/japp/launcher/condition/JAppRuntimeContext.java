package org.glavo.japp.launcher.condition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.jar.Manifest;

public final class JAppRuntimeContext {

    private static final Path PROJECT_DIRECTORY;
    private static final Path HOME;

    static {
        Manifest manifest;

        try (InputStream input = JAppRuntimeContext.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            manifest = new Manifest(input);
        } catch (IOException e) {
            throw new Error(e);
        }

        PROJECT_DIRECTORY = Paths.get(manifest.getMainAttributes().getValue("Project-Directory"));
        HOME = PROJECT_DIRECTORY.resolve(".japp");
    }

    private static String normalizeArch(String arch) {
        switch (arch) {
            case "x64":
            case "amd64":
                return "x86-64";
            default:
                return arch;
        }
    }

    private static String normalizeOS(String os) {
        if (os.startsWith("Windows")) {
            return "windows";
        }
        if (os.contains("mac")) {
            return "macos";
        }
        if (os.contains("linux")) {
            return "linux";
        }
        return os.toLowerCase(Locale.ROOT);
    }

    public static Path getHome() {
        return HOME;
    }

    public static JAppRuntimeContext fromCurrentEnvironment() {
        @SuppressWarnings("deprecation")
        int release = Runtime.version().major();

        return new JAppRuntimeContext(release,
                normalizeOS(System.getProperty("os.name")),
                normalizeArch(System.getProperty("os.arch")));
    }

    private final int release;
    private final String os;
    private final String arch;

    public JAppRuntimeContext(int release, String os, String arch) {
        this.release = release;
        this.os = os;
        this.arch = arch;
    }

    public int getRelease() {
        return release;
    }

    public String getOS() {
        return os;
    }

    public String getArch() {
        return arch;
    }
}
