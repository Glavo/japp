package org.glavo.japp.packer.compressor.classfile;

import org.glavo.japp.TODO;
import org.glavo.japp.packer.compressor.CompressResult;
import org.glavo.japp.packer.compressor.Compressor;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class ClassFileCompressor implements Compressor {

    private static final int MAGIC_NUMBER = 0xcafebabe;

    public static final ClassFileCompressor INSTANCE = new ClassFileCompressor();

    @Override
    public CompressResult compress(byte[] source) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(source);

        int magic = buffer.getInt();
        if (magic != MAGIC_NUMBER) {
            throw new IOException("Invalid magic number: " + Integer.toHexString(magic));
        }

        int minorVersion = Short.toUnsignedInt(buffer.getShort());
        int majorVersion = Short.toUnsignedInt(buffer.getShort());

        int cpCount = Short.toUnsignedInt(buffer.getShort());
        for (int i = 1; i < cpCount; i++) {
            byte tag = buffer.get();

            // TODO
        }
        throw new TODO();
    }
}
