package org.glavo.japp.launcher.condition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;

public final class JAppRuntimeContext {

    private static final Path HOME;

    static {
        Manifest manifest;

        try (InputStream input = JAppRuntimeContext.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            manifest = new Manifest(input);
        } catch (IOException e) {
            throw new Error(e);
        }

        HOME = Paths.get(manifest.getMainAttributes().getValue("JApp-Home"));
    }

    public static Path getHome() {
        return HOME;
    }

    public static JAppRuntimeContext fromCurrentEnvironment() {
        return new JAppRuntimeContext(Integer.getInteger("java.version"),
                System.getProperty("os.name"), System.getProperty("os.arch"));
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
