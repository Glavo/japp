package org.glavo.japp.packer.compressor;

import com.github.luben.zstd.Zstd;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
import org.glavo.japp.packer.compressor.classfile.ClassFileCompressor;

public final class Compressors {

    public static final Compressor DEFAULT = new DefaultCompressor();

    public static final Compressor CLASSFILE = new ClassFileCompressor();

    public static final Compressor ZSTD = (context, source) -> {
        byte[] res = new byte[ZstdUtils.maxCompressedLength(source.length)];
        long n = Zstd.compressByteArray(res, 0, res.length, source, 0, source.length, 8);
        return new CompressResult(CompressionMethod.ZSTD, res, 0, (int) n);
    };

    private Compressors() {
    }
}
