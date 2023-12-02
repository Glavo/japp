package org.glavo.japp.launcher;

import org.glavo.japp.TODO;
import org.glavo.japp.condition.ConditionParser;
import org.glavo.japp.platform.JAppRuntimeContext;
import org.glavo.japp.platform.JavaRuntime;
import org.glavo.japp.maven.MavenResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Manifest;

public final class Launcher {
    private static final String BOOT_LAUNCHER_MODULE = "org.glavo.japp.boot";

    private static String getBootLauncher() throws IOException {
        return new Manifest(Launcher.class.getResourceAsStream("/META-INF/MANIFEST.MF"))
                .getMainAttributes()
                .getValue("JApp-Boot");
    }

    private static void appendReferences(StringBuilder builder, int release, List<JAppResourceReference> references) throws Throwable {
        boolean isFirst = true;

        for (JAppResourceReference reference : references) {
            String name = reference.name;

            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(',');
            }

            builder.append(name).append(":");

            if (reference instanceof JAppResourceReference.Local) {
                JAppResourceReference.Local local = (JAppResourceReference.Local) reference;
                builder.append(Integer.toHexString(local.getIndex()));

                TreeMap<Integer, Integer> multiReleaseIndexes = local.getMultiReleaseIndexes();
                if (multiReleaseIndexes != null) {
                    for (Map.Entry<Integer, Integer> entry : multiReleaseIndexes.entrySet()) {
                        int r = entry.getKey();
                        int i = entry.getValue();

                        if (r <= release) {
                            builder.append('+').append(Integer.toHexString(i));
                        } else {
                            break;
                        }
                    }
                }
            } else if (reference instanceof JAppResourceReference.Maven) {
                JAppResourceReference.Maven maven = (JAppResourceReference.Maven) reference;

                Path file = MavenResolver.resolve(
                        maven.getRepository(),
                        maven.getGroup(),
                        maven.getArtifact(),
                        maven.getVersion(),
                        maven.getClassifier()
                ).toAbsolutePath().normalize();

                builder.append('E').append(file);
            } else {
                throw new TODO("Type: " + reference.getClass());
            }


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

        @SuppressWarnings("deprecation")
        int release = context.getJava().getVersion().major();

        boolean enablePreview = false;

        boolean isFirst = true;
        StringBuilder builder = new StringBuilder(80);
        if (!config.getEnableNativeAccess().isEmpty()) {


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
            appendReferences(builder, release, config.getModulePath());
            command.add(builder.toString());
        }

        if (!config.getClassPath().isEmpty()) {
            builder.setLength(0);
            builder.append("-Dorg.glavo.japp.classpath=");
            appendReferences(builder, release, config.getClassPath());
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
