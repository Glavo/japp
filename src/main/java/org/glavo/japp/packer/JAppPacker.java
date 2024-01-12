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
package org.glavo.japp.packer;

import org.glavo.japp.JAppProperties;
import org.glavo.japp.condition.ConditionParser;
import org.glavo.japp.io.LittleEndianDataOutput;
import org.glavo.japp.launcher.JAppConfigGroup;
import org.glavo.japp.launcher.Launcher;
import org.glavo.japp.packer.processor.ClassPathProcessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class JAppPacker {
    private static final class JAppConfigGroupBuilder {
        final JAppConfigGroup group = new JAppConfigGroup();
        final JAppConfigGroupBuilder parent;
        final List<JAppConfigGroupBuilder> children = new ArrayList<>();

        String classPath;
        String modulePath;

        public JAppConfigGroupBuilder(JAppConfigGroupBuilder parent) {
            this.parent = parent;
        }

        public void writeTo(JAppWriter writer) throws Throwable {
            ClassPathProcessor.process(writer, classPath, false);
            ClassPathProcessor.process(writer, modulePath, true);

            for (JAppConfigGroupBuilder child : children) {
                writer.beginConfigGroup(child.group);
                child.writeTo(writer);
                writer.endConfigGroup();
            }
        }
    }

    private JAppConfigGroupBuilder current = new JAppConfigGroupBuilder(null);

    private static String nextArg(String[] args, int index) {
        if (index < args.length - 1) {
            return args[index + 1];
        } else {
            System.err.println("Error: no value given for " + args[index]);
            System.exit(1);
            throw new AssertionError();
        }
    }

    public static void main(String[] args) throws Throwable {
        JAppPacker packer = new JAppPacker();
        Path outputFile = null;
        boolean appendBootJar = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-o": {
                    outputFile = Paths.get(nextArg(args, i++));
                    break;
                }
                case "-module-path":
                case "--module-path": {
                    packer.current.modulePath = nextArg(args, i++);
                    break;
                }
                case "-cp":
                case "-classpath":
                case "--classpath":
                case "-class-path":
                case "--class-path": {
                    packer.current.classPath = nextArg(args, i++);
                    break;
                }
                case "-m": {
                    packer.current.group.mainModule = nextArg(args, i++);
                    break;
                }
                case "--add-reads": {
                    String item = nextArg(args, i++);
                    packer.current.group.addReads.add(item);
                    break;
                }
                case "--add-exports": {
                    String item = nextArg(args, i++);
                    packer.current.group.addExports.add(item);
                    break;
                }
                case "--add-opens": {
                    String item = nextArg(args, i++);
                    packer.current.group.addOpens.add(item);
                    break;
                }
                case "--enable-native-access": {
                    String item = nextArg(args, i++);
                    packer.current.group.enableNativeAccess.add(item);
                    break;
                }
                case "--condition": {
                    String condition = nextArg(args, i++);
                    ConditionParser.parse(condition);
                    packer.current.group.condition = condition;
                    break;
                }
                case "--group": {
                    JAppConfigGroupBuilder subConfig = new JAppConfigGroupBuilder(packer.current);
                    packer.current.children.add(subConfig);
                    packer.current = subConfig;
                    break;
                }
                case "--end-group": {
                    if (packer.current.parent == null) {
                        System.err.println("Error: no open group");
                        System.exit(1);
                    }

                    packer.current = packer.current.parent;
                    break;
                }
                case "--embed-launcher": {
                    appendBootJar = true;
                    break;
                }
                default: {
                    if (arg.startsWith("-D")) {
                        String property = arg.substring("-D".length());

                        if (property.isEmpty() || property.indexOf('=') == 0) {
                            System.err.println("Error: JVM property name cannot be empty");
                            System.exit(1);
                        }

                        packer.current.group.jvmProperties.add(property);
                    } else if (arg.startsWith("--add-reads=")) {
                        packer.current.group.addReads.add(arg.substring("--add-reads=".length()));
                    } else if (arg.startsWith("--add-exports=")) {
                        packer.current.group.addExports.add(arg.substring("--add-exports=".length()));
                    } else if (arg.startsWith("--add-opens=")) {
                        packer.current.group.addOpens.add(arg.substring("--add-opens=".length()));
                    } else if (arg.startsWith("--enable-native-access=")) {
                        packer.current.group.enableNativeAccess.add(arg.substring("--enable-native-access=".length()));
                    } else if (arg.startsWith("-X")) {
                        packer.current.group.extraJvmOptions.add(arg);
                    } else if (arg.startsWith("-")) {
                        System.err.println("Error: Unrecognized option: " + arg);
                        System.exit(1);
                    } else {
                        packer.current.group.mainClass = arg;
                    }
                }
            }
        }

        if (packer.current.parent != null) {
            System.err.println("Error: group not ended");
            System.exit(1);
        }

        if (outputFile == null) {
            System.err.println("Error: miss output file");
            System.exit(1);
        }

        String header;
        try (InputStream input = JAppWriter.class.getResourceAsStream("header.sh")) {
            header = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("%japp.project.directory%", JAppProperties.getProjectDirectory().toString());
        }

        try (LittleEndianDataOutput output = LittleEndianDataOutput.of(
                FileChannel.open(outputFile, EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)))) {
            output.writeBytes(header.getBytes(StandardCharsets.UTF_8));

            try (JAppWriter writer = new JAppWriter(output, packer.current.group)) {
                packer.current.writeTo(writer);
            }

            if (appendBootJar) {
                embedLauncher(output);
            }
        }

        //noinspection ResultOfMethodCallIgnored
        outputFile.toFile().setExecutable(true);
    }

    private static void embedLauncher(LittleEndianDataOutput output) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (ZipOutputStream zipOut = new ZipOutputStream(buffer)) {
            try (ZipInputStream zipIn = new ZipInputStream(Launcher.class.getProtectionDomain().getCodeSource().getLocation().openStream())) {
                ZipEntry entry;
                while ((entry = zipIn.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (!name.startsWith("win/") && !name.startsWith("darwin/") && !name.startsWith("linux/") && !name.startsWith("freebsd/")
                        && !name.startsWith("com/github/luben/zstd/")) {
                        zipOut.putNextEntry(entry);
                        zipIn.transferTo(zipOut);
                    }
                }
            }

            try (ZipFile zipFile = new ZipFile(JAppProperties.getBootJar().toFile())) {
                ZipEntry moduleInfo = zipFile.getEntry("module-info.class");
                zipOut.putNextEntry(moduleInfo);

                try (InputStream input = zipFile.getInputStream(moduleInfo)) {
                    input.transferTo(zipOut);
                }
            }
        }

        output.writeBytes(buffer.toByteArray());
    }
}
