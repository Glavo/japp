package org.glavo.japp.launcher.condition;

import org.glavo.japp.TODO;
import org.glavo.japp.launcher.platform.Architecture;
import org.glavo.japp.launcher.platform.LibC;
import org.glavo.japp.launcher.platform.OperatingSystem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class JavaRuntime {

    private static final Map<Path, JavaRuntime> runtimes = new HashMap<>();

    private static void tryAddJava(Path javaHome) {
        try {
            Path realJavaHome = javaHome.toRealPath();
            if (runtimes.containsKey(realJavaHome)) {
                return;
            }

            JavaRuntime java = fromDir(realJavaHome);
            runtimes.put(realJavaHome, java);

        } catch (IOException ignored) {
        }
    }

    private static void searchIn(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    continue;
                }
                tryAddJava(path);
            }
        } catch (IOException ignored) {
        }
    }

    static {
        if (OperatingSystem.parseOperatingSystem(System.getProperty("os.name")) == OperatingSystem.LINUX) {
            searchIn(Paths.get("/usr/lib/jvm"));
            searchIn(JAppRuntimeContext.getHome().resolve("jvm"));
            tryAddJava(Paths.get(System.getProperty("java.home")));
        } else {
            throw new TODO("Currently only supports Linux");
        }

    }

    public static Collection<JavaRuntime> getAllJava() {
        return runtimes.values();
    }

    public static JavaRuntime fromDir(Path dir) throws IOException {
        Path releaseFile = dir.resolve("release");
        if (!Files.exists(releaseFile)) {
            throw new IOException("Missing release file");
        }

        LinkedHashMap<String, String> release = new LinkedHashMap<>();
        for (String line : Files.readAllLines(releaseFile)) {
            int idx = line.indexOf('=');
            if (idx <= 0) {
                if (line.isEmpty()) {
                    continue;
                }
                throw new IOException("Line: " + line);
            }

            String key = line.substring(0, idx);
            String value;
            if (idx == line.length() - 1) {
                value = "";
            } else if (line.charAt(idx + 1) == '"') {
                if (line.charAt(line.length() - 1) != '"' || line.length() < idx + 3) {
                    throw new IOException("Line: " + line);
                }
                value = line.substring(idx + 2, line.length() - 1);
            } else {
                value = line.substring(idx + 1);
            }

            if (release.put(key, value) != null) {
                throw new IOException("Duplicate key: " + key);
            }
        }

        OperatingSystem os;
        Architecture arch;
        LibC libc;
        Runtime.Version version;

        String osName = release.getOrDefault("OS_NAME", System.getProperty("os.name"));
        os = OperatingSystem.parseOperatingSystem(osName);

        String osArch = release.getOrDefault("OS_ARCH", System.getProperty("os.arch"));
        arch = Architecture.parseArchitecture(osArch);

        String libcName = release.get("LIBC");
        if (libcName != null) {
            libc = LibC.parseLibC(libcName);
        } else {
            libc = LibC.DEFAULT;
        }

        String javaVersion = release.get("JAVA_VERSION");
        version = Runtime.Version.parse(javaVersion);

        Path exec = os.findJavaExecutable(dir);
        return new JavaRuntime(exec, version, os, arch, libc);
    }

    private final Path exec;
    private final Runtime.Version version;
    private final OperatingSystem operatingSystem;
    private final Architecture architecture;
    private final LibC libc;

    public JavaRuntime(Path exec, Runtime.Version version, OperatingSystem operatingSystem, Architecture architecture, LibC libc) {
        this.exec = exec;
        this.version = version;
        this.operatingSystem = operatingSystem;
        this.architecture = architecture;
        this.libc = libc;
    }

    public Path getExec() {
        return exec;
    }

    public Runtime.Version getVersion() {
        return version;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public LibC getLibC() {
        return libc;
    }

    @Override
    public String toString() {
        return String.format("%s (version=%s, os=%s, arch=%s, libc=%s)",
                exec, version, operatingSystem, architecture, libc);
    }
}
