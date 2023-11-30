package org.glavo.japp.packer;

import com.github.luben.zstd.Zstd;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.JAppBootMetadata;
import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
import org.glavo.japp.launcher.JAppConfigGroup;
import org.glavo.japp.launcher.JAppResourceReference;
import org.glavo.japp.launcher.condition.ConditionParser;
import org.glavo.japp.packer.compressor.Compressor;
import org.glavo.japp.packer.compressor.Compressors;
import org.glavo.japp.packer.compressor.classfile.ByteArrayPoolBuilder;
import org.glavo.japp.util.ByteBufferOutputStream;
import org.glavo.japp.util.XxHash64;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public JAppConfigGroup current = root;

    private final List<Map<String, JAppResourceBuilder>> groups = new ArrayList<>();

    private final Compressor compressor = Compressors.DEFAULT;
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

    public Compressor getCompressor() {
        return compressor;
    }

    private int addGroup(Map<String, JAppResourceBuilder> group) {
        int index = groups.size();
        groups.add(group);
        return index;
    }

    public void addLocalReference(
            boolean isModulePath, String name,
            Map<String, JAppResourceBuilder> baseGroup, TreeMap<Integer, Map<String, JAppResourceBuilder>> multiGroups) {

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

    private void writeBootMetadata() throws IOException {
        output.writeInt(JAppBootMetadata.MAGIC_NUMBER);
        output.writeInt(groups.size());
        for (Map<String, JAppResourceBuilder> group : groups) {
            ByteBufferOutputStream groupBodyBuilder = new ByteBufferOutputStream();
            for (JAppResourceBuilder resource : group.values()) {
                resource.writeTo(groupBodyBuilder);
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
        output.writeBytes(current.toJson().toString().getBytes(StandardCharsets.UTF_8));
    }

    private void writeFileEnd(long metadataOffset, long bootMetadataOffset) throws IOException {
        long fileSize = output.getTotalBytes() + JAppConfigGroup.FILE_END_SIZE;

        // magic number
        output.writeInt(MAGIC_NUMBER);

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
            writeBootMetadata();

            long metadataOffset = getCurrentOffset();
            writeLauncherMetadata();

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
