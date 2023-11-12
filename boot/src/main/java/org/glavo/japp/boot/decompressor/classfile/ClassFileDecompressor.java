package org.glavo.japp.boot.decompressor.classfile;

import org.glavo.japp.TODO;
import org.glavo.japp.classfile.ClassFile;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class ClassFileDecompressor {
    public static byte[] decompress(ByteBuffer compressed, byte[] output) throws IOException {
        int magic = compressed.getInt();
        if (magic != ClassFile.MAGIC_NUMBER) {
            throw new IOException("Invalid magic number: " + Integer.toHexString(magic));
        }

        throw new TODO();
    }
}
