package org.glavo.japp.launcher;

import org.glavo.japp.TODO;
import org.glavo.japp.launcher.condition.ConditionParser;
import org.glavo.japp.launcher.platform.JAppRuntimeContext;
import org.glavo.japp.launcher.platform.JavaRuntime;
import org.glavo.japp.launcher.maven.MavenResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

public final class Launcher {
    private static final String BOOT_LAUNCHER_MODULE = "org.glavo.japp.boot";

    private static String getBootLauncher() throws IOException {
        return new Manifest(Launcher.class.getResourceAsStream("/META-INF/MANIFEST.MF"))
                .getMainAttributes()
                .getValue("JApp-Boot");
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

        JAppConfigGroup config = JAppConfigGroup.readFile(Paths.get(args[0]));
        JAppRuntimeContext context = JAppRuntimeContext.search(config);
        if (context == null) {
            System.err.println("Error: Unable to find suitable Java");
            System.err.println("Condition: " + ConditionParser.parse(config.condition));
            System.err.println("Java:");

            for (JavaRuntime java : JavaRuntime.getAllJava()) {
                System.err.println("  - " + java);
            }

            System.exit(1);
        }

        config.resolve(context);

        List<String> command = new ArrayList<>();
        command.add(context.getJava().getExec().toString());

        for (String property : config.getJvmProperties()) {
            command.add("-D" + property);
        }

        command.add("-Dorg.glavo.japp.file=" + args[0]);

        if (config.getBaseOffset() != 0) {
            command.add("-Dorg.glavo.japp.file.offset=" + Long.toHexString(config.getBaseOffset()));
        }

        command.add("-Dorg.glavo.japp.file.metadataOffset=" + Long.toHexString(config.getBootMetadataOffset()));

        int index = 0;
        for (String addReads : config.getAddReads()) {
            command.add("-Dorg.glavo.japp.addreads." + index++ + "=" + addReads);
        }

        index = 0;
        for (String addOpen : config.getAddOpens()) {
            command.add("-Dorg.glavo.japp.addopens." + index++ + "=" + addOpen);
        }

        index = 0;
        for (String addExport : config.getAddExports()) {
            command.add("-Dorg.glavo.japp.addexports." + index++ + "=" + addExport);
        }

        boolean enablePreview = false;

        boolean isFirst = true;
        StringBuilder builder = new StringBuilder(80);
        if (!config.getEnableNativeAccess().isEmpty()) {
            @SuppressWarnings("deprecation")
            int release = context.getJava().getVersion().major();

            if (release == 16) {
                command.add("-Dforeign.restricted=permit");
            } else if (release >= 17) {
                command.add("--enable-native-access=" + BOOT_LAUNCHER_MODULE);

                for (String module : config.getEnableNativeAccess()) {
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

        if (!config.getModulePath().isEmpty()) {
            builder.setLength(0);
            builder.append("-Dorg.glavo.japp.modules=");
            appendReferences(builder, config.getModulePath());
            command.add(builder.toString());
        }

        if (!config.getClassPath().isEmpty()) {
            builder.setLength(0);
            builder.append("-Dorg.glavo.japp.classpath=");
            appendReferences(builder, config.getClassPath());
            command.add(builder.toString());
        }

        if (config.getMainClass() != null) {
            command.add("-Dorg.glavo.japp.mainClass=" + config.getMainClass());
        }

        if (config.getMainModule() != null) {
            command.add("-Dorg.glavo.japp.mainModule=" + config.getMainModule());
        }

        Collections.addAll(command,
                "--module-path",
                getBootLauncher(),
                "--add-exports=java.base/jdk.internal.loader=" + BOOT_LAUNCHER_MODULE,
                "--add-exports=java.base/jdk.internal.module=" + BOOT_LAUNCHER_MODULE,
                "--add-exports=java.base/jdk.internal.misc=" + BOOT_LAUNCHER_MODULE,
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
