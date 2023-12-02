package org.glavo.japp;

import java.io.IOException;
import java.nio.ByteBuffer;

public enum CompressionMethod {
    NONE,
    CLASSFILE,
    ZSTD;

    private static final CompressionMethod[] METHODS = values();

    public static CompressionMethod of(int i) {
        return i >= 0 && i < METHODS.length ? METHODS[i] : null;
    }

    public static CompressionMethod readFrom(ByteBuffer buffer) throws IOException {
        byte id = buffer.get();
        if (id >= 0 && id < METHODS.length) {
            return METHODS[id];
        }

        throw new IOException(String.format("Unknown compression method: %02x", Byte.toUnsignedInt(id)));
    }

    public byte id() {
        return (byte) ordinal();
    }
}
