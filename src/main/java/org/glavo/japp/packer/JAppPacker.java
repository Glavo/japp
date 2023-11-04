package org.glavo.japp.packer;

import org.glavo.japp.boot.JAppBootMetadata;
import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.compress.Compressor;
import org.glavo.japp.launcher.JAppLauncherMetadata;
import org.glavo.japp.launcher.condition.ConditionParser;

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

public final class JAppPacker {

    private static final short MAJOR_VERSION = -1;
    private static final short MINOR_VERSION = 0;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(16 * 1024 * 1024);
    private final byte[] ba = new byte[8];
    private final ByteBuffer bb = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);

    private final JAppLauncherMetadata root = new JAppLauncherMetadata();

    private ArrayDeque<JAppLauncherMetadata> stack = new ArrayDeque<>();
    JAppLauncherMetadata current = root;

    final List<JAppResourceGroup> groups = new ArrayList<>();

    final Compressor compressor = Compressor.DEFAULT;

    long totalBytes = 0L;

    private boolean finished = false;

    private JAppPacker() {
    }

    void writeByte(byte b) throws IOException {
        buffer.write(b & 0xff);
        totalBytes += 1;
    }

    void writeShort(short s) throws IOException {
        bb.putShort(0, s);
        buffer.write(ba, 0, 2);
        totalBytes += 2;
    }

    void writeInt(int i) throws IOException {
        bb.putInt(0, i);
        buffer.write(ba, 0, 4);
        totalBytes += 4;
    }

    void writeLong(long l) throws IOException {
        bb.putLong(0, l);
        buffer.write(ba, 0, 8);
        totalBytes += 8;
    }

    void writeBytes(byte[] arr) throws IOException {
        writeBytes(arr, 0, arr.length);
    }

    void writeBytes(byte[] arr, int offset, int len) throws IOException {
        buffer.write(arr, offset, len);
        totalBytes += len;
    }

    private void writeFileEnd(long metadataOffset, long bootMetadataOffset) throws IOException {
        long fileSize = totalBytes + 48;

        // magic number
        ba[0] = 'J';
        ba[1] = 'A';
        ba[2] = 'P';
        ba[3] = 'P';
        writeBytes(ba, 0, 4);

        // version number
        writeShort(MAJOR_VERSION);
        writeShort(MINOR_VERSION);

        // flags
        writeLong(0L);

        // file size
        writeLong(fileSize);

        // metadata offset
        writeLong(metadataOffset);

        // boot metadata offset
        writeLong(bootMetadataOffset);

        // reserved
        writeLong(0L);

        if (totalBytes != fileSize) {
            throw new AssertionError();
        }
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        if (!finished) {
            finished = true;

            long bootMetadataOffset = totalBytes;
            byte[] bootMetadata = new JAppBootMetadata(groups).toJson().toString().getBytes(StandardCharsets.UTF_8);
            writeInt(bootMetadata.length);
            writeBytes(bootMetadata);

            long metadataOffset = totalBytes;
            writeBytes(current.toJson().toString().getBytes(StandardCharsets.UTF_8));
            writeFileEnd(metadataOffset, bootMetadataOffset);
        }

        this.buffer.writeTo(outputStream);
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

        if (outputFile == null) {
            System.err.println("Error: miss output file");
            System.exit(1);
        }

        String self = Paths.get(JAppPacker.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().toString();

        String header;
        try (InputStream input = JAppPacker.class.getResourceAsStream("header.sh")) {
            header = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("%java.home%", System.getProperty("java.home"))
                    .replace("%japp.launcher%", self);
        }

        try (OutputStream out = Files.newOutputStream(outputFile)) {
            out.write(header.getBytes(StandardCharsets.UTF_8));
            packer.writeTo(out);
        }

        outputFile.toFile().setExecutable(true);
    }
}
