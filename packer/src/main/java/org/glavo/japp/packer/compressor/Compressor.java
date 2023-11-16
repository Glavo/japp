package org.glavo.japp.packer.compressor;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.packer.JAppPacker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

@FunctionalInterface
public interface Compressor {

    Compressor DEFAULT = new DefaultCompressor();

    Compressor LZ4 = (context, source) -> {
        LZ4Compressor compressor = LZ4Factory.fastestJavaInstance().highCompressor();
        byte[] result = new byte[compressor.maxCompressedLength(source.length)];
        int len = compressor.compress(source, result);
        return new CompressResult(CompressionMethod.LZ4, result, 0, len);
    };

    Compressor DEFLATE = (context, source) -> {
        Deflater deflater = new Deflater();
        deflater.setInput(source);
        deflater.finish();

        byte[] res = new byte[source.length];
        int count = 0;

        while (!deflater.finished()) {
            if (count == res.length) {
                res = Arrays.copyOf(res, res.length * 2);
            }

            count += deflater.deflate(res, count, res.length - count);
        }

        deflater.end();

        return new CompressResult(CompressionMethod.DEFLATE, res, 0, count);
    };

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
