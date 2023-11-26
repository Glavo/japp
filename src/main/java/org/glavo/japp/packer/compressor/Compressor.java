package org.glavo.japp.packer.compressor;

import org.glavo.japp.packer.JAppPacker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;

@FunctionalInterface
public interface Compressor {

    CompressResult compress(JAppPacker packer, byte[] source) throws IOException;

    default CompressResult compress(JAppPacker packer, byte[] source, String ext) throws IOException {
        return compress(packer, source);
    }

    default CompressResult compress(JAppPacker packer, byte[] source, Path file, BasicFileAttributes attributes) throws IOException {
        String fileName = file.getFileName().toString();
        int idx = fileName.lastIndexOf('.');

        String ext = idx > 0 ? fileName.substring(idx + 1) : "";
        return compress(packer, source, ext);
    }

    default CompressResult compress(JAppPacker packer, byte[] source, ZipEntry entry) throws IOException {
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

        return compress(packer, source, ext);
    }
}
