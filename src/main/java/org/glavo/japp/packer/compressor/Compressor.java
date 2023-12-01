package org.glavo.japp.packer.compressor;

import org.glavo.japp.packer.JAppPacker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;

@FunctionalInterface
public interface Compressor {

    CompressResult compress(JAppPacker packer, byte[] source) throws IOException;

    default CompressResult compress(JAppPacker packer, byte[] source, String filePath) throws IOException {
        return compress(packer, source);
    }

    default CompressResult compress(JAppPacker packer, byte[] source, Path file, BasicFileAttributes attributes) throws IOException {
        return compress(packer, source, file.getFileName().toString());
    }

    default CompressResult compress(JAppPacker packer, byte[] source, ZipEntry entry) throws IOException {
        return compress(packer, source, entry.getName());
    }
}
