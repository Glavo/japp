package org.glavo.japp.packer;

import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.launcher.JAppResourceReference;
import org.glavo.japp.packer.compressor.CompressResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class LocalClassPathProcessor extends ClassPathProcessor {

    public static final LocalClassPathProcessor INSTANCE = new LocalClassPathProcessor();

    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";


    public static void addJar(JAppPacker packer, Path jar, boolean modulePath) throws IOException {
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
                        moduleName = ModuleInfoReader.readModuleName(mi);
                    }
                }
            }

            if (modulePath && moduleName == null) {
                moduleName = ModuleInfoReader.deriveAutomaticModuleName(jar.getFileName().toString());
            }

            // If the module name is not found, the file name is retained
            JAppResourceGroup group = new JAppResourceGroup(modulePath ? moduleName : jar.getFileName().toString());
            JAppResourceReference reference = new JAppResourceReference.Local(group.getName(), packer.groups.size());

            packer.groups.add(group);
            if (modulePath) {
                packer.current.modulePath.add(reference);
            } else {
                packer.current.classPath.add(reference);
            }

            // Then write all entries

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith("/")) {
                    continue;
                }

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

                CompressResult result = packer.compressor.compress(packer, buffer, entry);

                if (groupEntries == null) {
                    groupEntries = group.getResources();
                }

                groupEntries.put(name, new JAppResource(
                        name, packer.totalBytes, entry.getSize(),
                        result.getMethod(), result.getLength(),
                        entry.getLastAccessTime(), entry.getLastModifiedTime(), entry.getCreationTime()
                ));

                packer.writeBytes(result.getCompressedData(), result.getOffset(), result.getLength());
            }
        }
    }

    public static void addDir(JAppPacker packer, Path dir, boolean modulePath) throws IOException {
        String name;
        if (modulePath) {
            try (InputStream input = Files.newInputStream(dir.resolve("module-info.class"))) {
                name = ModuleInfoReader.readModuleName(input);
            }
        } else {
            name = null;
        }

        JAppResourceGroup group = new JAppResourceGroup(name);
        JAppResourceReference reference = new JAppResourceReference.Local(null, packer.groups.size());
        packer.groups.add(group);
        if (modulePath) {
            packer.current.modulePath.add(reference);
        } else {
            packer.current.classPath.add(reference);
        }

        Path absoluteDir = dir.toAbsolutePath().normalize();
        Files.walkFileTree(absoluteDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String path = absoluteDir.relativize(file).toString().replace('\\', '/');
                byte[] data = Files.readAllBytes(file);
                CompressResult result = packer.compressor.compress(packer, Files.readAllBytes(file), file, attrs);
                group.getResources().put(path, new JAppResource(
                        path, packer.totalBytes, data.length,
                        result.getMethod(), result.getLength(),
                        attrs.lastAccessTime(), attrs.lastModifiedTime(), attrs.creationTime()
                ));
                packer.writeBytes(result.getCompressedData(), result.getOffset(), result.getLength());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void process(JAppPacker packer, String path, boolean isModulePath, Map<String, String> options) throws IOException {
        String name = options.remove("name");

        if (!options.isEmpty()) {
            throw new IllegalArgumentException("Unrecognized options: " + options.keySet());
        }

        boolean scanFiles = false;
        if (!isModulePath && (path.endsWith("/*") || path.endsWith("\\*"))) {
            scanFiles = true;
            path = path.substring(0, path.length() - 2);
        }

        Path p = Paths.get(path);
        BasicFileAttributes attributes = Files.readAttributes(p, BasicFileAttributes.class);

        if (scanFiles && !attributes.isDirectory()) {
            throw new IllegalArgumentException(path + " is not a directory");
        }

        if (attributes.isDirectory()) {
            if (isModulePath) {
                Path mi = p.resolve("module-info.class");
                if (!Files.exists(mi)) {
                    scanFiles = true;
                }
            }

            if (scanFiles) {
                if (name != null) {
                    throw new IllegalArgumentException("Name should not be set for multiple files");
                }

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {
                    for (Path file : stream) {
                        String fileName = file.getFileName().toString();

                        if (Files.isRegularFile(file) && fileName.endsWith(".jar")) {
                            addJar(packer, file, isModulePath);
                        }
                    }
                }
            } else {
                addDir(packer, p, isModulePath);
            }
        } else if (p.getFileName().toString().endsWith(".jar")) {
            addJar(packer, p, isModulePath);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + p);
        }
    }
}
