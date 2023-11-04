package org.glavo.japp.launcher;

import org.glavo.japp.TODO;
import org.glavo.japp.launcher.condition.JAppRuntimeContext;
import org.glavo.japp.maven.MavenResolver;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Launcher {
    private static final String BOOT_LAUNCHER_MODULE = "org.glavo.japp";

    private static Path getBootLauncher() throws URISyntaxException {
        return Paths.get(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static void appendReferences(StringBuilder builder, List<JAppResourceReference> references) throws Throwable {
        boolean isFirst = true;

        for (JAppResourceReference reference : references) {
            String name = reference.name;

            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(',');
            }

            String refStr;
            if (reference instanceof JAppResourceReference.Local) {
                refStr = Integer.toHexString(((JAppResourceReference.Local) reference).getIndex());
            } else if (reference instanceof JAppResourceReference.Maven) {
                JAppResourceReference.Maven maven = (JAppResourceReference.Maven) reference;

                Path file = MavenResolver.resolve(
                        maven.getRepository(),
                        maven.getGroup(),
                        maven.getArtifact(),
                        maven.getVersion(),
                        maven.getClassifier()
                ).toAbsolutePath().normalize();

                refStr = "E" + file;
            } else {
                throw new TODO("Type: " + reference.getClass());
            }

            builder.append(name).append(":").append(refStr);
        }
    }

    public static void main(String[] args) throws Throwable {
        if (args.length < 1) {
            throw new TODO("Help Message");
        }

        String javaName = System.getProperty("os.name").contains("Win") ? "java.exe" : "java";

        JAppRuntimeContext context = JAppRuntimeContext.fromCurrentEnvironment(); // TODO

        JAppLauncherMetadata metadata = JAppLauncherMetadata.readFile(Paths.get(args[0]));

        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", javaName).toString());

        for (String property : metadata.getJvmProperties()) {
            command.add("-D" + property);
        }

        command.add("-Dorg.glavo.japp.file=" + args[0]);

        if (metadata.getBaseOffset() != 0) {
            command.add("-Dorg.glavo.japp.file.offset=" + Long.toHexString(metadata.getBaseOffset()));
        }

        command.add("-Dorg.glavo.japp.file.metadataOffset=" + Long.toHexString(metadata.getBootMetadataOffset()));

        int index = 0;
        for (String addReads : metadata.getAddReads()) {
            command.add("-Dorg.glavo.japp.addreads." + index++ + "=" + addReads);
        }

        index = 0;
        for (String addOpen : metadata.getAddOpens()) {
            command.add("-Dorg.glavo.japp.addopens." + index++ + "=" + addOpen);
        }

        index = 0;
        for (String addExport : metadata.getAddExports()) {
            command.add("-Dorg.glavo.japp.addexports." + index++ + "=" + addExport);
        }

        boolean enablePreview = false;

        boolean isFirst = true;
        StringBuilder builder = new StringBuilder(80);
        if (!metadata.getEnableNativeAccess().isEmpty()) {
            int release = context.getRelease();

            if (release == 16) {
                command.add("-Dforeign.restricted=permit");
            } else if (release >= 17) {
                command.add("--enable-native-access=" + BOOT_LAUNCHER_MODULE);

                for (String module : metadata.getEnableNativeAccess()) {
                    if (module.equals("ALL-UNNAMED")) {
                        command.add("--enable-native-access=ALL-UNNAMED");
                    } else {
                        if (isFirst) {
                            builder.append("-Dorg.glavo.japp.enableNativeAccess=");
                        } else {
                            builder.append(',');
                        }

                        isFirst = false;
                        builder.append(module);
                    }
                }

                if (!isFirst) {
                    command.add(builder.toString());
                }
            }

            if (release <= 21) {
                enablePreview = true;
            }
        }

        if (enablePreview) {
            command.add("--enable-preview");
        }

        if (!metadata.getModulePath().isEmpty()) {
            builder.setLength(0);
            builder.append("-Dorg.glavo.japp.modules=");
            appendReferences(builder, metadata.getModulePath());
            command.add(builder.toString());
        }

        if (!metadata.getClassPath().isEmpty()) {
            builder.setLength(0);
            builder.append("-Dorg.glavo.japp.classpath=");
            appendReferences(builder, metadata.getClassPath());
            command.add(builder.toString());
        }

        if (metadata.getMainClass() != null) {
            command.add("-Dorg.glavo.japp.mainClass=" + metadata.getMainClass());
        }

        if (metadata.getMainModule() != null) {
            command.add("-Dorg.glavo.japp.mainModule=" + metadata.getMainModule());
        }

        Collections.addAll(command,
                "--module-path",
                getBootLauncher().toString(),
                "--add-exports=java.base/jdk.internal.loader=" + BOOT_LAUNCHER_MODULE,
                "--add-exports=java.base/jdk.internal.module=" + BOOT_LAUNCHER_MODULE,
                "--add-opens=java.base/jdk.internal.loader=" + BOOT_LAUNCHER_MODULE,
                "--add-opens=java.base/java.lang=" + BOOT_LAUNCHER_MODULE,
                "--module",
                BOOT_LAUNCHER_MODULE + "/org.glavo.japp.boot.BootLauncher"
        );

        for (int i = 1; i < args.length; i++) {
            command.add(args[i]);
        }

        System.exit(new ProcessBuilder(command).inheritIO().start().waitFor());
    }
}
