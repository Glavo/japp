package org.glavo.japp.packer;

import org.glavo.japp.boot.JAppBootMetadata;
import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.launcher.JAppConfigGroup;
import org.glavo.japp.launcher.JAppResourceReference;
import org.glavo.japp.launcher.condition.ConditionParser;
import org.glavo.japp.packer.compressor.Compressor;
import org.glavo.japp.packer.compressor.Compressors;
import org.glavo.japp.packer.compressor.classfile.ByteArrayPoolBuilder;
import org.glavo.japp.util.ByteBufferBuilder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.jar.Manifest;

public final class JAppPacker {

    private static final byte[] MAGIC_NUMBER = {'J', 'A', 'P', 'P'};
    private static final short MAJOR_VERSION = -1;
    private static final short MINOR_VERSION = 0;

    private final ByteBufferBuilder output = new ByteBufferBuilder(32 * 1024 * 1024);

    {
        output.writeBytes(MAGIC_NUMBER);
    }

    private final JAppConfigGroup root = new JAppConfigGroup();

    private final ArrayDeque<JAppConfigGroup> stack = new ArrayDeque<>();
    JAppConfigGroup current = root;

    final List<JAppResourceGroup> groups = new ArrayList<>();

    final Compressor compressor = Compressors.DEFAULT;
    final ByteArrayPoolBuilder pool = new ByteArrayPoolBuilder();

    private boolean finished = false;

    private JAppPacker() {
    }

    public ByteBufferBuilder getOutput() {
        return output;
    }

    public long getCurrentOffset() {
        return output.getTotalBytes();
    }

    public ByteArrayPoolBuilder getPool() {
        return pool;
    }

    private int addGroup(JAppResourceGroup group) {
        int index = groups.size();
        groups.add(group);
        return index;
    }

    public void addLocalReference(
            boolean isModulePath, String name,
            JAppResourceGroup baseGroup, TreeMap<Integer, JAppResourceGroup> multiGroups) {

        int baseIndex = addGroup(baseGroup);
        TreeMap<Integer, Integer> multiIndexes;
        if (multiGroups != null && !multiGroups.isEmpty()) {
            multiIndexes = new TreeMap<>();
            multiGroups.forEach((i, g) -> multiIndexes.put(i, addGroup(g)));
        } else {
            multiIndexes = null;
        }

        JAppResourceReference.Local ref = new JAppResourceReference.Local(name, baseIndex, multiIndexes);
        if (isModulePath) {
            current.getModulePath().add(ref);
        } else {
            current.getClassPath().add(ref);
        }
    }

    private void writeFileEnd(long metadataOffset, long bootMetadataOffset) throws IOException {
        long fileSize = output.getTotalBytes() + JAppConfigGroup.FILE_END_SIZE;

        // magic number
        output.writeBytes(MAGIC_NUMBER);

        // version number
        output.writeShort(MAJOR_VERSION);
        output.writeShort(MINOR_VERSION);

        // flags
        output.writeLong(0L);

        // file size
        output.writeLong(fileSize);

        // launcher metadata offset
        output.writeLong(metadataOffset);

        // boot metadata offset
        output.writeLong(bootMetadataOffset);

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
            byte[] bootMetadata = JAppBootMetadata.toJson(groups, pool.toByteArray()).toString().getBytes(StandardCharsets.UTF_8);
            output.writeInt(bootMetadata.length);
            output.writeBytes(bootMetadata);

            long metadataOffset = getCurrentOffset();
            output.writeBytes(current.toJson().toString().getBytes(StandardCharsets.UTF_8));
            writeFileEnd(metadataOffset, bootMetadataOffset);
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
                    packer.current.subConfigs.add(group);
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
