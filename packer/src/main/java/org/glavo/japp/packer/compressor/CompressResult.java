package org.glavo.japp.packer.compressor;

import org.glavo.japp.CompressionMethod;

import java.util.Arrays;

public final class CompressResult {
    private final CompressionMethod method;
    private final byte[] compressedData;

    private final int offset;
    private final int length;

    public CompressResult(byte[] compressedData) {
        this(CompressionMethod.NONE, compressedData, 0, compressedData.length);
    }

    public CompressResult(CompressionMethod method, byte[] compressedData) {
        this(method, compressedData, 0, compressedData.length);
    }

    public CompressResult(CompressionMethod method, byte[] compressedData, int offset, int length) {
        this.method = method;
        this.compressedData = compressedData;
        this.offset = offset;
        this.length = length;
    }

    public CompressionMethod getMethod() {
        return method;
    }

    public byte[] getCompressedData() {
        return compressedData;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "CompressResult{" +
               "method=" + method +
               ", compressedData=" + Arrays.toString(compressedData) +
               ", offset=" + offset +
               ", length=" + length +
               '}';
    }
}
