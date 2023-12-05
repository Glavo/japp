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
import org.glavo.japp.boot.JAppBootArgs;
import org.glavo.japp.condition.ConditionParser;
import org.glavo.japp.platform.JAppRuntimeContext;
import org.glavo.japp.platform.JavaRuntime;
import org.glavo.japp.maven.MavenResolver;
import org.glavo.japp.util.ByteBufferOutputStream;

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

    private static void writeStringListField(ByteBufferOutputStream out, JAppBootArgs.Field field, List<String> list) throws IOException {
        if (list.isEmpty()) {
            return;
        }

        out.writeByte(field.id());
        out.writeInt(list.size());
        for (String string : list) {
            out.writeString(string);
        }
    }

    private static void writeClassOrModulePath(
            ByteBufferOutputStream out, JAppBootArgs.Field field,
            int release, List<JAppResourceGroupReference> references) throws Throwable {
        if (references.isEmpty()) {
            return;
        }

        out.writeByte(field.id());
        for (JAppResourceGroupReference reference : references) {
            String name = reference.name;

            if (reference instanceof JAppResourceGroupReference.Local) {
                JAppResourceGroupReference.Local local = (JAppResourceGroupReference.Local) reference;

                out.writeByte(JAppBootArgs.ID_RESOLVED_REFERENCE_LOCAL);
                out.writeString(name);
                out.writeInt(local.getIndex());

                TreeMap<Integer, Integer> multiReleaseIndexes = local.getMultiReleaseIndexes();
                if (multiReleaseIndexes != null) {
                    for (Map.Entry<Integer, Integer> entry : multiReleaseIndexes.entrySet()) {
                        int r = entry.getKey();
                        int i = entry.getValue();

                        if (r <= release) {
                            out.writeInt(i);
                        } else {
                            break;
                        }
                    }
                }

                out.writeInt(-1);
            } else if (reference instanceof JAppResourceGroupReference.Maven) {
                JAppResourceGroupReference.Maven maven = (JAppResourceGroupReference.Maven) reference;

                Path file = MavenResolver.resolve(
                        maven.getRepository(),
                        maven.getGroup(),
                        maven.getArtifact(),
                        maven.getVersion(),
                        maven.getClassifier()
                ).toAbsolutePath().normalize();

                out.writeByte(JAppBootArgs.ID_RESOLVED_REFERENCE_EXTERNAL);
                out.writeString(name);
                out.writeString(file.toString());
            } else {
                throw new TODO("Type: " + reference.getClass());
            }
        }
        out.writeByte(JAppBootArgs.ID_RESOLVED_REFERENCE_END);
    }

    public static void main(String[] args) throws Throwable {
        if (args.length < 1) {
            throw new TODO("Help Message");
        }

        Path file = Paths.get(args[0]).toAbsolutePath().normalize();

        JAppLauncherMetadata config = JAppLauncherMetadata.readFile(file);
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

        @SuppressWarnings("deprecation")
        int release = context.getJava().getVersion().major();

        boolean enablePreview = false;

        for (String property : group.getJvmProperties()) {
            command.add("-D" + property);
        }

        try (ByteBufferOutputStream argsBuilder = new ByteBufferOutputStream()) {
            argsBuilder.writeString(file.toString());
            argsBuilder.writeLong(config.getBaseOffset());
            argsBuilder.writeLong(config.getBootMetadataOffset());
            argsBuilder.writeLong(config.getBootMetadataSize());

            writeStringListField(argsBuilder, JAppBootArgs.Field.ADD_READS, group.getAddReads());
            writeStringListField(argsBuilder, JAppBootArgs.Field.ADD_OPENS, group.getAddOpens());
            writeStringListField(argsBuilder, JAppBootArgs.Field.ADD_EXPORTS, group.getAddExports());

            if (!group.getEnableNativeAccess().isEmpty()) {
                if (release == 16) {
                    command.add("-Dforeign.restricted=permit");
                } else if (release >= 17) {
                    command.add("--enable-native-access=" + BOOT_LAUNCHER_MODULE);

                    List<String> list;
                    if (group.getEnableNativeAccess().contains("ALL-UNNAMED")) {
                        command.add("--enable-native-access=ALL-UNNAMED");
                        list = new ArrayList<>();
                        for (String module : group.getEnableNativeAccess()) {
                            if (!module.equals("ALL-UNNAMED")) {
                                list.add(module);
                            }
                        }
                    } else {
                        list = group.getEnableNativeAccess();
                    }

                    writeStringListField(argsBuilder, JAppBootArgs.Field.ENABLE_NATIVE_ACCESS, list);
                }

                if (release <= 21) {
                    enablePreview = true;
                }
            }

            writeClassOrModulePath(argsBuilder, JAppBootArgs.Field.MODULE_PATH, release, group.getModulePath());
            writeClassOrModulePath(argsBuilder, JAppBootArgs.Field.CLASS_PATH, release, group.getClassPath());

            if (group.getMainClass() != null) {
                argsBuilder.writeByte(JAppBootArgs.Field.MAIN_CLASS.id());
                argsBuilder.writeString(group.getMainClass());
            }

            if (group.getMainModule() != null) {
                argsBuilder.writeByte(JAppBootArgs.Field.MAIN_MODULE.id());
                argsBuilder.writeString(group.getMainModule());
            }

            argsBuilder.writeByte(JAppBootArgs.Field.END.id());

            command.add("-Dorg.glavo.japp.boot.args=" + Base64.getEncoder().encodeToString(argsBuilder.toByteArray()));
        }

        if (enablePreview) {
            command.add("--enable-preview");
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
