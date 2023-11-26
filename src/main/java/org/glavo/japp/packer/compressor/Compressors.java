package org.glavo.japp.packer.compressor;

import com.github.luben.zstd.Zstd;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.packer.compressor.classfile.ClassFileCompressor;

import java.util.Arrays;
import java.util.zip.Deflater;

public final class Compressors {

    public static final Compressor DEFAULT = new DefaultCompressor();

    public static final Compressor CLASSFILE = new ClassFileCompressor();

    public static final Compressor LZ4 = (context, source) -> {
        LZ4Compressor compressor = LZ4Factory.fastestJavaInstance().highCompressor();
        byte[] result = new byte[compressor.maxCompressedLength(source.length)];
        int len = compressor.compress(source, result);
        return new CompressResult(CompressionMethod.LZ4, result, 0, len);
    };

    public static final Compressor ZSTD = (context, source) -> {
        int maxCompressedSize = source.length + (source.length >>> 8);

        if (source.length < 128 * 1024) {
            maxCompressedSize += (128 * 1024 - source.length) >>> 11;
        }

        byte[] res = new byte[maxCompressedSize];
        long n = Zstd.compressByteArray(res, 0, res.length, source, 0, source.length, 8);
        return new CompressResult(CompressionMethod.ZSTD, res, 0, (int) n);
    };

    public static final Compressor DEFLATE = (context, source) -> {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
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

    private Compressors() {
    }
}
