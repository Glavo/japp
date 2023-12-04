/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.launcher;

import org.glavo.japp.JAppConfigGroup;
import org.glavo.japp.JAppResourceGroupReference;
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

    private static void appendReferences(StringBuilder builder, int release, List<JAppResourceGroupReference> references) throws Throwable {
        boolean isFirst = true;

        for (JAppResourceGroupReference reference : references) {
            String name = reference.name;

            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(',');
            }

            builder.append(name).append(":");

            if (reference instanceof JAppResourceGroupReference.Local) {
                JAppResourceGroupReference.Local local = (JAppResourceGroupReference.Local) reference;
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
            } else if (reference instanceof JAppResourceGroupReference.Maven) {
                JAppResourceGroupReference.Maven maven = (JAppResourceGroupReference.Maven) reference;

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

        JAppLauncherMetadata config = JAppLauncherMetadata.readFile(Paths.get(args[0]));
        JAppConfigGroup group = config.getGroup();

        JAppRuntimeContext context = JAppRuntimeContext.search(group);
        if (context == null) {
            System.err.println("Error: Unable to find suitable Java");
            System.err.println("Condition: " + ConditionParser.parse(group.condition));
            System.err.println("Java:");

            for (JavaRuntime java : JavaRuntime.getAllJava()) {
                System.err.println("  - " + java);
            }

            System.exit(1);
        }

        config.getGroup().resolve(context);

        List<String> command = new ArrayList<>();
        command.add(context.getJava().getExec().toString());

        for (String property : group.getJvmProperties()) {
            command.add("-D" + property);
        }

        command.add("-Dorg.glavo.japp.file=" + args[0]);

        if (config.getBaseOffset() != 0) {
            command.add("-Dorg.glavo.japp.file.offset=" + Long.toHexString(config.getBaseOffset()));
        }

        command.add("-Dorg.glavo.japp.file.metadata.offset=" + Long.toHexString(config.getBootMetadataOffset()));
        command.add("-Dorg.glavo.japp.file.metadata.size=" + Long.toHexString(config.getBootMetadataSize()));

        int index = 0;
        for (String addReads : group.getAddReads()) {
            command.add("-Dorg.glavo.japp.addreads." + index++ + "=" + addReads);
        }

        index = 0;
        for (String addOpen : group.getAddOpens()) {
            command.add("-Dorg.glavo.japp.addopens." + index++ + "=" + addOpen);
        }

        index = 0;
        for (String addExport : group.getAddExports()) {
            command.add("-Dorg.glavo.japp.addexports." + index++ + "=" + addExport);
        }

        @SuppressWarnings("deprecation")
        int release = context.getJava().getVersion().major();

        boolean enablePreview = false;

        boolean isFirst = true;
        StringBuilder builder = new StringBuilder(80);
        if (!group.getEnableNativeAccess().isEmpty()) {


            if (release == 16) {
                command.add("-Dforeign.restricted=permit");
            } else if (release >= 17) {
                command.add("--enable-native-access=" + BOOT_LAUNCHER_MODULE);

                for (String module : group.getEnableNativeAccess()) {
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

        if (!group.getModulePath().isEmpty()) {
            builder.setLength(0);
            builder.append("-Dorg.glavo.japp.modules=");
            appendReferences(builder, release, group.getModulePath());
            command.add(builder.toString());
        }

        if (!group.getClassPath().isEmpty()) {
            builder.setLength(0);
            builder.append("-Dorg.glavo.japp.classpath=");
            appendReferences(builder, release, group.getClassPath());
            command.add(builder.toString());
        }

        if (group.getMainClass() != null) {
            command.add("-Dorg.glavo.japp.mainClass=" + group.getMainClass());
        }

        if (group.getMainModule() != null) {
            command.add("-Dorg.glavo.japp.mainModule=" + group.getMainModule());
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
                BOOT_LAUNCHER_MODULE
        );

        for (int i = 1; i < args.length; i++) {
            command.add(args[i]);
        }

        System.exit(new ProcessBuilder(command).inheritIO().start().waitFor());
    }
}
