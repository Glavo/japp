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

import com.github.luben.zstd.Zstd;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.JAppBootMetadata;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.boot.JAppResourceField;
import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
import org.glavo.japp.JAppConfigGroup;
import org.glavo.japp.JAppResourceGroupReference;
import org.glavo.japp.condition.ConditionParser;
import org.glavo.japp.launcher.JAppLauncherMetadata;
import org.glavo.japp.packer.compressor.Compressor;
import org.glavo.japp.packer.compressor.Compressors;
import org.glavo.japp.packer.compressor.classfile.ByteArrayPoolBuilder;
import org.glavo.japp.packer.processor.ClassPathProcessor;
import org.glavo.japp.util.ByteBufferOutputStream;
import org.glavo.japp.util.XxHash64;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.jar.Manifest;

public final class JAppPacker {

    private static final int MAGIC_NUMBER = 0x5050414a;
    private static final short MAJOR_VERSION = -1;
    private static final short MINOR_VERSION = 0;

    private final ByteBufferOutputStream output = new ByteBufferOutputStream(32 * 1024 * 1024);
    {
        output.writeInt(MAGIC_NUMBER);
    }

    private final JAppConfigGroup root = new JAppConfigGroup();

    private final ArrayDeque<JAppConfigGroup> stack = new ArrayDeque<>();
    private JAppConfigGroup current = root;

    final List<Map<String, JAppResourceInfo>> groups = new ArrayList<>();

    final Compressor compressor = Compressors.DEFAULT;
    private final ByteArrayPoolBuilder pool = new ByteArrayPoolBuilder();

    private boolean finished = false;

    private JAppPacker() {
    }

    public ByteBufferOutputStream getOutput() {
        return output;
    }

    public long getCurrentOffset() {
        return output.getTotalBytes();
    }

    public ByteArrayPoolBuilder getPool() {
        return pool;
    }

    public JAppResourcesWriter createResourcesWriter(String name, boolean isModulePath) {
        return new JAppResourcesWriter(this, name, isModulePath);
    }

    public void addReference(JAppResourceGroupReference reference, boolean isModulePath) {
        Objects.requireNonNull(reference);

        if (isModulePath) {
            current.modulePath.add(reference);
        } else {
            current.classPath.add(reference);
        }
    }

    private static void writeResourceFileTimeField(ByteBufferOutputStream output, JAppResourceField field, FileTime time) {
        if (time != null) {
            output.writeByte(field.id());
            output.writeLong(time.toMillis());
        }
    }
    private static void writeResource(JAppResourceInfo resource, ByteBufferOutputStream groupBodyBuilder) {
        byte[] nameBytes = resource.name.getBytes(StandardCharsets.UTF_8);

        groupBodyBuilder.writeByte(JAppResource.MAGIC_NUMBER);
        groupBodyBuilder.writeByte(resource.method.id());
        groupBodyBuilder.writeUnsignedShort(nameBytes.length);
        groupBodyBuilder.writeInt(0);
        groupBodyBuilder.writeLong(resource.size);
        groupBodyBuilder.writeLong(resource.compressedSize);
        groupBodyBuilder.writeLong(resource.offset);
        groupBodyBuilder.writeBytes(nameBytes);

        if (resource.checksum != null) {
            groupBodyBuilder.writeByte(JAppResourceField.CHECKSUM.id());
            groupBodyBuilder.writeLong(resource.checksum);
        }

        writeResourceFileTimeField(groupBodyBuilder, JAppResourceField.FILE_CREATE_TIME, resource.creationTime);
        writeResourceFileTimeField(groupBodyBuilder, JAppResourceField.FILE_LAST_MODIFIED_TIME, resource.lastModifiedTime);
        writeResourceFileTimeField(groupBodyBuilder, JAppResourceField.FILE_LAST_ACCESS_TIME, resource.lastAccessTime);

        groupBodyBuilder.writeByte(JAppResourceField.END.id());
    }

    private void writeBootMetadata() throws IOException {
        output.writeInt(JAppBootMetadata.MAGIC_NUMBER);
        output.writeInt(groups.size());
        pool.writeTo(output);
        for (Map<String, JAppResourceInfo> group : groups) {
            ByteBufferOutputStream groupBodyBuilder = new ByteBufferOutputStream();
            for (JAppResourceInfo resource : group.values()) {
                writeResource(resource, groupBodyBuilder);
            }
            byte[] groupBody = groupBodyBuilder.toByteArray();

            CompressionMethod method = null;
            byte[] compressed = null;
            int compressedLength = -1;

            if (groupBody.length >= 16) {
                byte[] res = new byte[ZstdUtils.maxCompressedLength(groupBody.length)];
                long n = Zstd.compressByteArray(res, 0, res.length, groupBody, 0, groupBody.length, 8);
                if (n < groupBody.length - 4) {
                    method = CompressionMethod.ZSTD;
                    compressed = res;
                    compressedLength = (int) n;
                }
            }

            if (method == null) {
                method = CompressionMethod.NONE;
                compressed = groupBody;
                compressedLength = groupBody.length;
            }

            long checksum = XxHash64.hash(groupBody);

            output.writeByte(JAppResourceGroup.MAGIC_NUMBER);
            output.writeByte(method.id());
            output.writeShort((short) 0); // reserved
            output.writeInt(groupBody.length);
            output.writeInt(compressedLength);
            output.writeInt(group.size());
            output.writeLong(checksum);
            output.writeBytes(compressed, 0, compressedLength);
        }
    }

    private void writeLauncherMetadata() throws IOException {
        current.writeTo(output);
    }

    private void writeFileEnd(long bootMetadataOffset, long launcherMetadataOffset) throws IOException {
        long fileSize = output.getTotalBytes() + JAppLauncherMetadata.FILE_END_SIZE;

        // magic number
        output.writeInt(MAGIC_NUMBER);

        // version number
        output.writeShort(MAJOR_VERSION);
        output.writeShort(MINOR_VERSION);

        // flags
        output.writeLong(0L);

        // file size
        output.writeLong(fileSize);

        // boot metadata offset
        output.writeLong(bootMetadataOffset);

        // launcher metadata offset
        output.writeLong(launcherMetadataOffset);

        // reserved
        output.writeLong(0L);
        output.writeLong(0L);
        output.writeLong(0L);

        if (output.getTotalBytes() != fileSize) {
            throw new AssertionError();
        }
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        if (!finished) {
            finished = true;

            long bootMetadataOffset = getCurrentOffset();
            writeBootMetadata();

            long launcherMetadataOffset = getCurrentOffset();
            writeLauncherMetadata();

            writeFileEnd(bootMetadataOffset, launcherMetadataOffset);
        }

        this.output.writeTo(outputStream);
    }

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
        Path outputFile = null;

        JAppPacker packer = new JAppPacker();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-o": {
                    outputFile = Paths.get(nextArg(args, i++));
                    break;
                }
                case "-module-path":
                case "--module-path": {
                    ClassPathProcessor.process(packer, nextArg(args, i++), true);
                    break;
                }
                case "-cp":
                case "-classpath":
                case "--classpath":
                case "-class-path":
                case "--class-path": {
                    ClassPathProcessor.process(packer, nextArg(args, i++), false);
                    break;
                }
                case "-m": {
                    packer.current.mainModule = nextArg(args, i++);
                    break;
                }
                case "--add-reads": {
                    packer.current.addReads.add(nextArg(args, i++));
                    break;
                }
                case "--add-exports": {
                    packer.current.addExports.add(nextArg(args, i++));
                    break;
                }
                case "--add-opens": {
                    packer.current.addOpens.add(nextArg(args, i++));
                    break;
                }
                case "--enable-native-access": {
                    packer.current.enableNativeAccess.add(nextArg(args, i++));
                    break;
                }
                case "--condition": {
                    packer.current.condition = nextArg(args, i++);

                    try {
                        ConditionParser.parse(packer.current.condition);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Illegal condition: " + packer.current.condition);
                        System.exit(1);
                    }

                    break;
                }
                case "--group": {
                    JAppConfigGroup group = new JAppConfigGroup();
                    packer.stack.push(group);
                    packer.current.subGroups.add(group);
                    packer.current = group;
                    break;
                }
                case "--end-group": {
                    if (packer.stack.isEmpty()) {
                        System.err.println("Error: no open group");
                        System.exit(1);
                    }

                    packer.stack.pop();
                    packer.current = packer.stack.isEmpty() ? packer.root : packer.stack.peek();
                    break;
                }
                default: {
                    if (arg.startsWith("-D")) {
                        String property = arg.substring("-D".length());

                        if (property.isEmpty() || property.indexOf('=') == 0) {
                            System.err.println("Error: JVM property name cannot be empty");
                            System.exit(1);
                        }

                        packer.current.jvmProperties.add(property);
                    } else if (arg.startsWith("--add-reads=")) {
                        packer.current.addReads.add(arg.substring("--add-reads=".length()));
                    } else if (arg.startsWith("--add-exports=")) {
                        packer.current.addExports.add(arg.substring("--add-exports=".length()));
                    } else if (arg.startsWith("--add-opens=")) {
                        packer.current.addOpens.add(arg.substring("--add-opens=".length()));
                    } else if (arg.startsWith("--enable-native-access=")) {
                        packer.current.enableNativeAccess.add(arg.substring("--enable-native-access=".length()));
                    } else if (arg.startsWith("-")) {
                        System.err.println("Error: Unrecognized option: " + arg);
                        System.exit(1);
                    } else {
                        if (packer.current.mainClass != null) {
                            System.err.println("Error: Duplicate main class");
                            System.exit(1);
                        }

                        packer.current.mainClass = arg;
                    }
                }
            }
        }

        if (!packer.stack.isEmpty()) {
            System.err.println("Error: group not ended");
            System.exit(1);
        }

        if (outputFile == null) {
            System.err.println("Error: miss output file");
            System.exit(1);
        }

        String header;
        try (InputStream input = JAppPacker.class.getResourceAsStream("header.sh")) {
            header = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("%japp.project.directory%", new Manifest(JAppPacker.class.getResourceAsStream("/META-INF/MANIFEST.MF"))
                            .getMainAttributes()
                            .getValue("Project-Directory"));
        }

        try (OutputStream out = Files.newOutputStream(outputFile)) {
            out.write(header.getBytes(StandardCharsets.UTF_8));
            packer.writeTo(out);
        }

        outputFile.toFile().setExecutable(true);
    }
}
