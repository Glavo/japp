package org.glavo.japp.packer.compressor.classfile;

import org.glavo.japp.TODO;
import org.glavo.japp.classfile.ClassFile;
import org.glavo.japp.packer.compressor.CompressContext;
import org.glavo.japp.packer.compressor.CompressResult;
import org.glavo.japp.packer.compressor.Compressor;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class ClassFileCompressor implements Compressor {

    public static final ClassFileCompressor INSTANCE = new ClassFileCompressor();

    private static final byte A_MARK = 1;

    private static int maxCompressedSize(int input) {
        return input + input / 255 + 16;
    }

    @Override
    public CompressResult compress(CompressContext context, byte[] source) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(source);

        byte[] output = new byte[maxCompressedSize(source.length)];
        ByteBuffer outputBuffer = ByteBuffer.wrap(output);

        int magic = buffer.getInt();
        if (magic != ClassFile.MAGIC_NUMBER) {
            throw new IOException("Invalid magic number: " + Integer.toHexString(magic));
        }

        int minorVersion = Short.toUnsignedInt(buffer.getShort());
        int majorVersion = Short.toUnsignedInt(buffer.getShort());
        int cpCount = Short.toUnsignedInt(buffer.getShort());

        outputBuffer.putInt(magic);
        outputBuffer.putShort((short) minorVersion);
        outputBuffer.putShort((short) majorVersion);
        outputBuffer.putShort((short) cpCount);

        byte[][] strings = new byte[cpCount][];
        byte[] marks = new byte[cpCount];

        for (int i = 1; i < cpCount; i++) {
            int tag = Byte.toUnsignedInt(buffer.get());

            if (tag == ClassFile.CONSTANT_Utf8) {
                int len = Short.toUnsignedInt(buffer.getShort());
                byte[] bytes = new byte[len];
                buffer.get(bytes);
                strings[i] = bytes;
            } else {
                int size;
                if (tag < ClassFile.CONSTANT_SIZE.length && (size = ClassFile.CONSTANT_SIZE[tag]) > 0) {
                    buffer.get(outputBuffer.array(), outputBuffer.position(), size);
                    outputBuffer.position(outputBuffer.position() + size);
                } else {
                    throw new IOException("Invalid constant pool item: tag=" + tag);
                }
            }

            // TODO
        }
        throw new TODO();
    }
}
