package org.glavo.japp.platform;

import org.glavo.japp.launcher.JAppConfigGroup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static Path getHome() {
        return HOME;
    }

    public static JAppRuntimeContext search(JAppConfigGroup config) {
        for (JavaRuntime java : JavaRuntime.getAllJava()) {
            JAppRuntimeContext context = new JAppRuntimeContext(java);
            if (config.canApply(context)) {
                return context;
            }
        }

        return null;
    }

    private final JavaRuntime java;

    public JAppRuntimeContext(JavaRuntime java) {
        this.java = java;
    }

    public JavaRuntime getJava() {
        return java;
    }
}
