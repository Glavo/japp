package org.glavo.japp.launcher;

import org.glavo.japp.JAppMetadata;
import org.glavo.japp.TODO;
import org.glavo.japp.JAppRuntimeContext;

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

    public static void main(String[] args) throws Throwable {
        if (args.length < 1) {
            throw new TODO("Help Message");
        }

        String javaName = System.getProperty("os.name").contains("Win") ? "java.exe" : "java";

        JAppRuntimeContext context = JAppRuntimeContext.fromCurrentEnvironment(); // TODO

        JAppMetadata metadata = JAppMetadata.readFile(Paths.get(args[0]));

        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", javaName).toString());

        for (String property : metadata.getJvmProperties()) {
            command.add("-D" + property);
        }

        int index = 0;
        for (String addOpen : metadata.getAddOpens()) {
            command.add("-Dorg.glavo.japp.addopens." + index++ + "=" + addOpen);
        }

        index = 0;
        for (String addExport : metadata.getAddExports()) {
            command.add("-Dorg.glavo.japp.addexports." + index++ + "=" + addExport);
        }

        if (!metadata.getEnableNativeAccess().isEmpty()) {
            int release = context.getRelease();

            if (release == 16) {
                command.add("-Dforeign.restricted=permit");
            } else if (release >= 17) {
                command.add("--enable-native-access=" + BOOT_LAUNCHER_MODULE);

                StringBuilder builder = new StringBuilder();
                boolean isFirst = true;
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
        }

        Collections.addAll(command,
                "--module-path",
                getBootLauncher().toString(),
                "-Dorg.glavo.japp.file=" + args[0],
                "--add-exports=java.base/jdk.internal.loader=" + BOOT_LAUNCHER_MODULE,
                "--add-exports=java.base/jdk.internal.module=" + BOOT_LAUNCHER_MODULE,
                "--add-opens=java.base/jdk.internal.loader=" + BOOT_LAUNCHER_MODULE,
                "--add-opens=java.base/java.lang=" + BOOT_LAUNCHER_MODULE,
                "--module",
                BOOT_LAUNCHER_MODULE + "/org.glavo.japp.launcher.BootLauncher"
        );

        for (int i = 1; i < args.length; i++) {
            command.add(args[i]);
        }

        System.exit(new ProcessBuilder(command).inheritIO().start().waitFor());
    }
}
