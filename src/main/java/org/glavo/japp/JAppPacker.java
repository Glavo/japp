package org.glavo.japp;

import java.io.*;
import java.lang.module.ModuleDescriptor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class JAppPacker {

    private static final short MAJOR_VERSION = -1;
    private static final short MINOR_VERSION = 0;

    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(16 * 1024 * 1024);
    private final byte[] ba = new byte[8];
    private final ByteBuffer bb = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);

    private final JAppMetadata root = new JAppMetadata();

    private ArrayDeque<JAppMetadata> stack = new ArrayDeque<>();
    private JAppMetadata current = root;

    private long totalBytes = 0L;

    private int unnamedCounter = 0;

    private boolean finished = false;

    private JAppPacker() {
    }

    private void writeByte(byte b) throws IOException {
        buffer.write(b & 0xff);
        totalBytes += 1;
    }

    private void writeShort(short s) throws IOException {
        bb.putShort(0, s);
        buffer.write(ba, 0, 2);
        totalBytes += 2;
    }

    private void writeInt(int i) throws IOException {
        bb.putInt(0, i);
        buffer.write(ba, 0, 4);
        totalBytes += 4;
    }

    private void writeLong(long l) throws IOException {
        bb.putLong(0, l);
        buffer.write(ba, 0, 8);
        totalBytes += 8;
    }

    private void writeBytes(byte[] arr) throws IOException {
        writeBytes(arr, 0, arr.length);
    }

    private void writeBytes(byte[] arr, int offset, int len) throws IOException {
        buffer.write(arr, offset, len);
        totalBytes += len;
    }

    private static String readModuleName(InputStream moduleInfo) throws IOException {
        // TODO: Java 8
        return ModuleDescriptor.read(moduleInfo).name();
    }

    public void addJar(Path jar, boolean modulePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(jar.toFile())) {
            Attributes attributes = null;

            ZipEntry manifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
            if (manifestEntry != null) {
                try (InputStream input = zipFile.getInputStream(manifestEntry)) {
                    attributes = new Manifest(input).getMainAttributes();
                }
            }

            boolean multiRelease;
            String moduleName = null;

            if (attributes != null) {
                multiRelease = Boolean.parseBoolean(attributes.getValue("Multi-Release"));
                if (modulePath) {
                    moduleName = attributes.getValue("Automatic-Module-Name");
                }
            } else {
                multiRelease = false;
            }

            if (modulePath && moduleName == null) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    String name = entry.getName();
                    if (name.equals("module-info.class")) {
                        // OK
                    } else if (multiRelease && name.startsWith(MULTI_RELEASE_PREFIX) && name.endsWith("/module-info.class")) {
                        // e.g. META-INF/versions/9/module-info.class
                        String[] elements = name.split("/");
                        if (elements.length != 4) {
                            continue;
                        }

                        try {
                            if (Integer.parseInt(elements[2]) < 9) {
                                continue;
                            }
                        } catch (NumberFormatException ignored) {
                            continue;
                        }

                    } else {
                        continue;
                    }

                    // parse module-info.class

                    try (InputStream mi = zipFile.getInputStream(entry)) {
                        moduleName = readModuleName(mi);
                    }
                }
            }

            if (modulePath && moduleName == null) {
                throw new TODO();
            }

            // If the module name is not found, the file name is retained
            JAppClasspathItem item = new JAppClasspathItem(modulePath ? moduleName : jar.getFileName().toString());
            if (modulePath) {
                this.current.modulePath.put(item.getName(), item);
            } else {
                this.current.classPath.put(item.getName(), item);
            }

            // Then write all entries

            byte[] buffer = new byte[8192];

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                Map<String, JAppResource> itemEntries = null;

                if (multiRelease && name.startsWith(MULTI_RELEASE_PREFIX)) {
                    int idx = name.indexOf('/', MULTI_RELEASE_PREFIX.length());

                    if (idx > MULTI_RELEASE_PREFIX.length() && idx < name.length() - 1) {
                        String ver = name.substring(MULTI_RELEASE_PREFIX.length(), idx);
                        try {
                            int v = Integer.parseInt(ver);
                            if (v > 9) {
                                itemEntries = item.getMultiRelease(v);
                                name = name.substring(idx + 1);
                            }

                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                if (itemEntries == null) {
                    itemEntries = item.getResources();
                }

                itemEntries.put(name, new JAppResource(name, totalBytes, entry.getSize(), entry.getCreationTime(), entry.getLastModifiedTime()));

                try (InputStream in = zipFile.getInputStream(entry)) {
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        writeBytes(buffer, 0, n);
                    }
                }
            }
        }
    }

    public void addDir(Path dir, boolean modulePath) throws IOException {
        if (modulePath) {
            throw new TODO();
        }

        JAppClasspathItem item = new JAppClasspathItem("$unnamed$" + unnamedCounter++);
        if (modulePath) {
            this.current.modulePath.put(item.getName(), item);
        } else {
            this.current.classPath.put(item.getName(), item);
        }
        Files.walkFileTree(dir.toAbsolutePath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String path = dir.relativize(file).toString();

                byte[] bytes = Files.readAllBytes(file);
                item.getResources().put(path, new JAppResource(path, totalBytes, bytes.length, attrs.creationTime(), attrs.lastModifiedTime()));
                writeBytes(bytes);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void writeMetadata() throws IOException {
        writeBytes(current.toJson().toString().getBytes(StandardCharsets.UTF_8));
    }

    private void writeFileEnd(long metadataOffset) throws IOException {
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

        // reserved
        writeLong(0L);
        writeLong(0L);

        if (totalBytes != fileSize) {
            throw new AssertionError();
        }
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        if (!finished) {
            finished = true;

            long metadataOffset = totalBytes;
            writeMetadata();
            writeFileEnd(metadataOffset);
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

    public static void main(String[] args) throws IOException {
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
                    String mp = nextArg(args, i++);

                    String[] elements = mp.split(File.pathSeparator);
                    for (String element : elements) {
                        packer.addJar(Paths.get(element), true);
                    }
                    break;
                }
                case "-cp":
                case "-classpath":
                case "--classpath":
                case "-class-path":
                case "--class-path": {
                    String cp = nextArg(args, i++);

                    String[] elements = cp.split(File.pathSeparator);
                    for (String element : elements) {
                        Path path = Paths.get(element);
                        if (Files.isDirectory(path)) {
                            packer.addDir(path, false);
                        } else {
                            packer.addJar(path, false);
                        }
                    }
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

        try (OutputStream out = Files.newOutputStream(outputFile)) {
            packer.writeTo(out);
        }
    }
}
