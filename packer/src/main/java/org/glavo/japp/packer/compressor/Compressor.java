package org.glavo.japp.packer.compressor;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.glavo.japp.boot.CompressionMethod;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

@FunctionalInterface
public interface Compressor {

    Compressor DEFAULT = new DefaultCompressor();

    Compressor LZ4 = source -> {
        LZ4Compressor compressor = LZ4Factory.fastestJavaInstance().highCompressor();
        byte[] result = new byte[compressor.maxCompressedLength(source.length)];
        int len = compressor.compress(source, result);
        return new CompressResult(CompressionMethod.LZ4, result, 0, len);
    };

    Compressor DEFLATE = source -> {
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

    CompressResult compress(byte[] source);

    default CompressResult compress(byte[] source, String ext) {
        return compress(source);
    }

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
