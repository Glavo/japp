package org.glavo.japp.packer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Packer implements Closeable {

    private static final short MAJOR_VERSION = -1;
    private static final short MINOR_VERSION = 0;

    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";

    private final byte[] ba = new byte[8];
    private final ByteBuffer bb = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);

    private final List<JarMetadata> modulePath = new ArrayList<>();
    private final List<JarMetadata> classPath = new ArrayList<>();

    private final OutputStream outputStream;
    private long totalBytes = 0L;

    public Packer(OutputStream outputStream) {
        this.outputStream = Objects.requireNonNull(outputStream);
    }

    private void writeByte(byte b) throws IOException {
        outputStream.write(b & 0xff);
        totalBytes += 1;
    }

    private void writeShort(short s) throws IOException {
        bb.putShort(0, s);
        outputStream.write(ba, 0, 2);
        totalBytes += 2;
    }

    private void writeInt(int i) throws IOException {
        bb.putInt(0, i);
        outputStream.write(ba, 0, 4);
        totalBytes += 4;
    }

    private void writeLong(long l) throws IOException {
        bb.putLong(0, l);
        outputStream.write(ba, 0, 8);
        totalBytes += 8;
    }

    private void writeBytes(byte[] arr) throws IOException {
        writeBytes(arr, 0, arr.length);
    }

    private void writeBytes(byte[] arr, int offset, int len) throws IOException {
        outputStream.write(arr, offset, len);
        totalBytes += len;
    }

    private static String readModuleName(InputStream moduleInfo) throws IOException {
        // TODO: Java 8
        return ModuleDescriptor.read(moduleInfo).name();
    }

    public void writeJar(Path jar, boolean modulePath) throws IOException {
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

            // If the module name is not found, the file name is retained
            JarMetadata metadata = new JarMetadata(moduleName == null ? jar.getFileName().toString() : null, moduleName);

            // Then write all entries

            byte[] buffer = new byte[8192];

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                List<JarMetadata.Entry> metadataEntries = null;

                if (multiRelease && name.startsWith(MULTI_RELEASE_PREFIX)) {
                    int idx = name.indexOf('/', MULTI_RELEASE_PREFIX.length());

                    if (idx > MULTI_RELEASE_PREFIX.length() && idx < name.length() - 1) {
                        String ver = name.substring(MULTI_RELEASE_PREFIX.length(), idx);
                        try {
                            int v = Integer.parseInt(ver);
                            if (v > 9) {
                                metadataEntries = metadata.getMultiReleaseEntries(v);
                                name = name.substring(idx + 1);
                            }

                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                if (metadataEntries == null) {
                    metadataEntries = metadata.getEntries();
                }

                metadataEntries.add(new JarMetadata.Entry(name, totalBytes, entry.getSize(), entry.getCreationTime(), entry.getLastModifiedTime()));

                try (InputStream in = zipFile.getInputStream(entry)) {
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        writeBytes(buffer, 0, n);
                    }
                }
            }
        }
    }

    private void writeMetadata() throws IOException {
        JSONObject res = new JSONObject();

        JSONArray modulePath = new JSONArray();
        JSONArray classPath = new JSONArray();

        for (JarMetadata metadata : this.modulePath) {
            modulePath.put(metadata.toJson());
        }

        for (JarMetadata metadata : this.classPath) {
            classPath.put(metadata.toJson());
        }

        res.put("modulePath", modulePath);
        res.put("classPath", classPath);

        writeBytes(res.toString().getBytes(StandardCharsets.UTF_8));
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

    @Override
    public void close() throws IOException {
        long metadataOffset = totalBytes;
        writeMetadata();
        writeFileEnd(metadataOffset);
    }

    public static void main(String[] args) {
        List<Path> modulePath = new ArrayList<>();
        List<Path> classPath = new ArrayList<>();
        Path outputFile = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-module-path":
                case "--module-path": {

                }


            }

        }

    }
}
