package org.glavo.japp.launcher.condition;

import org.glavo.japp.TODO;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class JavaRuntime {

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

    public enum Architecture {
        X86(false, "x86"),
        X86_64(true, "x86-64"),
        IA64(true, "IA-64"),
        SPARC(false, "SPARC"),
        SPARCV9(true, "SPARC V9"),
        ARM32(false, "ARM32"),
        ARM64(true, "ARM64"),
        MIPS(false, "MIPS"),
        MIPS64(true, "MIPS64"),
        MIPSEL(false, "MIPSel"),
        MIPS64EL(true, "MIPS64el"),
        PPC(false, "PowerPC (Big-Endian)"),
        PPC64(true, "PowerPC-64 (Big-Endian)"),
        PPCLE(false, "PowerPC (Little-Endian)"),
        PPC64LE(true, "PowerPC-64 (Little-Endian)"),
        S390(false, "S390"),
        S390X(true, "S390x"),
        RISCV32(false, "RISC-V 32"),
        RISCV64(true, "RISC-V 64"),
        LOONGARCH32(false, "LoongArch32"),
        LOONGARCH64(true, "LoongArch64");

        private final String checkedName;
        private final String displayName;
        private final boolean is64Bit;

        Architecture(boolean is64Bit, String displayName) {
            this.checkedName = this.name().toLowerCase(Locale.ROOT).replace("_", "-");
            this.displayName = displayName;
            this.is64Bit = is64Bit;
        }

        public boolean is64Bit() {
            return is64Bit;
        }

        public String getCheckedName() {
            return checkedName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum LibC {
        DEFAULT, MUSL;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static OperatingSystem parseOperatingSystem(String name) {
        name = name.trim().toLowerCase(Locale.ROOT);

        if (name.contains("win"))
            return OperatingSystem.WINDOWS;
        else if (name.contains("mac"))
            return OperatingSystem.MACOS;
        else if (name.contains("linux"))
            return OperatingSystem.LINUX;
        else
            throw new IllegalArgumentException(name);
    }

    public static Architecture parseArchitecture(String value) {
        value = value.trim().toLowerCase(Locale.ROOT);

        switch (value) {
            case "x8664":
            case "x86-64":
            case "x86_64":
            case "amd64":
            case "ia32e":
            case "em64t":
            case "x64":
                return Architecture.X86_64;
            case "x8632":
            case "x86-32":
            case "x86_32":
            case "x86":
            case "i86pc":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
            case "ia32":
            case "x32":
                return Architecture.X86;
            case "arm64":
            case "aarch64":
                return Architecture.ARM64;
            case "arm":
            case "arm32":
                return Architecture.ARM32;
            case "mips64":
                return Architecture.MIPS64;
            case "mips64el":
                return Architecture.MIPS64EL;
            case "mips":
            case "mips32":
                return Architecture.MIPS;
            case "mipsel":
            case "mips32el":
                return Architecture.MIPSEL;
            case "riscv64":
                return Architecture.RISCV64;
            case "ia64":
            case "ia64w":
            case "itanium64":
                return Architecture.IA64;
            case "sparcv9":
            case "sparc64":
                return Architecture.SPARCV9;
            case "sparc":
            case "sparc32":
                return Architecture.SPARC;
            case "ppc64":
            case "powerpc64":
                return Architecture.PPC64;
            case "ppc64le":
            case "powerpc64le":
                return Architecture.PPC64LE;
            case "ppc":
            case "ppc32":
            case "powerpc":
            case "powerpc32":
                return Architecture.PPC;
            case "ppcle":
            case "ppc32le":
            case "powerpcle":
            case "powerpc32le":
                return Architecture.PPCLE;
            case "s390":
                return Architecture.S390;
            case "s390x":
                return Architecture.S390X;
            case "loongarch32":
                return Architecture.LOONGARCH32;
            case "loongarch64":
                return Architecture.LOONGARCH64;
            default:
                if (value.startsWith("armv7")) {
                    return Architecture.ARM32;
                }
                if (value.startsWith("armv8") || value.startsWith("armv9")) {
                    return Architecture.ARM64;
                }
        }

        throw new IllegalArgumentException();
    }

    public static LibC parseLibC(String value) {
        switch (value) {
            case "":
            case "default":
            case "gnu":
                return LibC.DEFAULT;
            case "musl":
                return LibC.MUSL;
            default:
                throw new IllegalArgumentException(value);
        }
    }

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
        if (parseOperatingSystem(System.getProperty("os.name")) == OperatingSystem.LINUX) {
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
        os = parseOperatingSystem(osName);

        String osArch = release.getOrDefault("OS_ARCH", System.getProperty("os.arch"));
        arch = parseArchitecture(osArch);

        String libcName = release.get("LIBC");
        if (libcName != null) {
            libc = parseLibC(libcName);
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
