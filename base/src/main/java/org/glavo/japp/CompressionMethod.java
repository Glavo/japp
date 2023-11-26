package org.glavo.japp;

public enum CompressionMethod {
    NONE,
    CLASSFILE,
    LZ4,
    ZSTD,
    DEFLATE;

    private static final CompressionMethod[] METHODS = values();

    public static CompressionMethod of(int i) {
        return i >= 0 && i < METHODS.length ? METHODS[i] : null;
    }
}
