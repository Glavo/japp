package org.glavo.japp.compress;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;

@FunctionalInterface
public interface Compressor {

    Compressor DEFAULT = new DefaultCompressor();

    CompressResult compress(byte[] source, String ext);

    default CompressResult compress(byte[] source, Path file, BasicFileAttributes attributes) {
        String fileName = file.getFileName().toString();
        int idx = fileName.lastIndexOf('.');

        String ext = idx > 0 ? fileName.substring(idx + 1) : "";
        return compress(source, ext);
    }

    default CompressResult compress(byte[] source, ZipEntry entry) {
        int idx = entry.getName().lastIndexOf('.');

        String ext;
        if (idx > 0) {
            int separator = entry.getName().lastIndexOf('/');
            if (separator < idx) {
                ext = entry.getName().substring(idx + 1);
            } else {
                ext = "";
            }
        } else {
            ext = "";
        }

        return compress(source, ext);
    }
}
