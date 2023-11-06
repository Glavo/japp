package org.glavo.japp.compress;

import org.glavo.japp.TODO;
import org.glavo.japp.compress.lz4.LZ4Compressor;

import java.util.zip.Deflater;

public enum CompressionMethod {
    NONE {
        @Override
        public CompressResult compress(byte[] source) {
            return new CompressResult(NONE, source);
        }
    },
    CLASSFILE {
        @Override
        public CompressResult compress(byte[] source) {
            throw new TODO();
        }
    },
    LZ4 {
        @Override
        public CompressResult compress(byte[] source) {
            byte[] result = new byte[LZ4Compressor.maxCompressedLength(source.length)];
            int len = LZ4Compressor.getHC().compress(source, result);
            return new CompressResult(LZ4, result, 0, len);
        }
    },
    DEFLATE {
        @Override
        public CompressResult compress(byte[] source) {
            Deflater deflater = new Deflater();
            deflater.setInput(source);
            deflater.finish();

            byte[] res = new byte[source.length];
            int count = 0;

            while (!deflater.finished() && count < res.length) {
                count += deflater.deflate(res, count, res.length - count);
            }

            deflater.end();

            return new CompressResult(CompressionMethod.DEFLATE, res, 0, count);
        }
    };

    public abstract CompressResult compress(byte[] source);
}
