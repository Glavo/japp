package org.glavo.japp;

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
        return new JAppRuntimeContext(Integer.getInteger("java.version"));
    }
    
    private final int release;

    public JAppRuntimeContext(int release) {
        this.release = release;
    }

    public int getRelease() {
        return release;
    }
}
