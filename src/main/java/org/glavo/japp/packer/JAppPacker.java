package org.glavo.japp.packer;

import org.glavo.japp.TODO;
import org.glavo.japp.boot.JAppBootMetadata;
import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.compress.CompressResult;
import org.glavo.japp.compress.Compressor;
import org.glavo.japp.launcher.JAppLauncherMetadata;
import org.glavo.japp.launcher.JAppResourceReference;

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

    private final JAppLauncherMetadata root = new JAppLauncherMetadata();

    private ArrayDeque<JAppLauncherMetadata> stack = new ArrayDeque<>();
    private JAppLauncherMetadata current = root;

    private final List<JAppResourceGroup> groups = new ArrayList<>();

    private final Compressor compressor = Compressor.DEFAULT;

    private long totalBytes = 0L;

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
            JAppResourceGroup group = new JAppResourceGroup(modulePath ? moduleName : jar.getFileName().toString());
            JAppResourceReference reference = new JAppResourceReference.Local(group.getName(), groups.size());

            groups.add(group);
            if (modulePath) {
                this.current.modulePath.add(reference);
            } else {
                this.current.classPath.add(reference);
            }

            // Then write all entries

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                Map<String, JAppResource> groupEntries = null;

                if (multiRelease && name.startsWith(MULTI_RELEASE_PREFIX)) {
                    int idx = name.indexOf('/', MULTI_RELEASE_PREFIX.length());

                    if (idx > MULTI_RELEASE_PREFIX.length() && idx < name.length() - 1) {
                        String ver = name.substring(MULTI_RELEASE_PREFIX.length(), idx);
                        try {
                            int v = Integer.parseInt(ver);
                            if (v > 9) {
                                groupEntries = group.getMultiRelease(v);
                                name = name.substring(idx + 1);
                            }

                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                byte[] buffer = new byte[(int) entry.getSize()];
                try (InputStream in = zipFile.getInputStream(entry)) {
                    int count = 0;
                    int n;
                    while ((n = in.read(buffer, count, buffer.length - count)) > 0) {
                        count += n;
                    }

                    assert count == buffer.length;
                }

                CompressResult result = compressor.compress(buffer, entry);

                if (groupEntries == null) {
                    groupEntries = group.getResources();
                }

                groupEntries.put(name, new JAppResource(
                        name, totalBytes, entry.getSize(),
                        entry.getLastAccessTime(), entry.getLastModifiedTime(), entry.getCreationTime(),
                        result.getMethod(), result.getLength()));

                writeBytes(result.getCompressedData(), result.getOffset(), result.getLength());
            }
        }
    }

    public void addDir(Path dir, boolean modulePath) throws IOException {
        if (modulePath) {
            throw new TODO();
        }

        JAppResourceGroup group = new JAppResourceGroup(null);
        JAppResourceReference reference = new JAppResourceReference.Local(null, groups.size());
        groups.add(group);
        if (modulePath) {
            throw new TODO();
        } else {
            this.current.classPath.add(reference);
        }
        Files.walkFileTree(dir.toAbsolutePath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String path = dir.relativize(file).toString().replace('\\', '/');
                byte[] data = Files.readAllBytes(file);
                CompressResult result = compressor.compress(Files.readAllBytes(file), file, attrs);
                group.getResources().put(path, new JAppResource(
                        path, totalBytes, data.length,
                        attrs.lastAccessTime(), attrs.lastModifiedTime(), attrs.creationTime(),
                        result.getMethod(), result.getLength()
                ));
                writeBytes(result.getCompressedData(), result.getOffset(), result.getLength());
                return FileVisitResult.CONTINUE;
            }
        });
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
