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

    private static int maxCompressedSize(int input) {
        return input + input / 2 + 16;
    }

    @Override
    public CompressResult compress(CompressContext context, byte[] source) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(source);

        int magic = buffer.getInt();
        if (magic != ClassFile.MAGIC_NUMBER) {
            throw new IOException("Invalid magic number: " + Integer.toHexString(magic));
        }

        int minorVersion = Short.toUnsignedInt(buffer.getShort());
        int majorVersion = Short.toUnsignedInt(buffer.getShort());
        int cpCount = Short.toUnsignedInt(buffer.getShort());

        byte[] tags = new byte[cpCount];
        int[] positions = new int[cpCount];

        // scan constant pool
        for (int i = 1; i < cpCount; i++) {
            final byte tag = buffer.get();
            final int position = positions[i] = buffer.position();
            final int len;
            if (tag == ClassFile.CONSTANT_Utf8) {
                len = Short.toUnsignedInt(buffer.getShort()) + 2;
                buffer.position(position + len);

                if (tags[i] == 0) {
                    tags[i] = tag;
                }
            } else if (tag < ClassFile.CONSTANT_SIZE.length && tag > 0 && (len = ClassFile.CONSTANT_SIZE[tag]) > 0) {
                tags[i] = tag;

                if (tag == ClassFile.CONSTANT_NameAndType || tag == ClassFile.CONSTANT_MethodType) {
                    if (tag == ClassFile.CONSTANT_NameAndType) {
                        //noinspection unused
                        int nameIndex = Short.toUnsignedInt(buffer.getShort());
                    }

                    int descriptorIndex = Short.toUnsignedInt(buffer.getShort());

                    if (tags[descriptorIndex] != 0 && tags[descriptorIndex] != ClassFile.CONSTANT_Utf8) {
                        throw new IOException("Constant #" + descriptorIndex + " is not CONSTANT_Utf8");
                    }

                    tags[descriptorIndex] = ClassFile.CONSTANT_EXTERNAL_Utf8;
                } else {
                    if (tag == ClassFile.CONSTANT_Long || tag == ClassFile.CONSTANT_Double) {
                        i++;
                    }

                    buffer.position(position + len);
                }
            } else {
                throw new IOException("Invalid constant pool item: tag=" + tag);
            }


            // TODO
        }

        int tailPosition = buffer.position();
        int tailLen = buffer.remaining();

        // TODO

        int arrayOffset = buffer.arrayOffset();
        byte[] output = new byte[maxCompressedSize(source.length)];
        ByteBuffer outputBuffer = ByteBuffer.wrap(output);

        outputBuffer.putInt(magic);
        outputBuffer.putShort((short) minorVersion);
        outputBuffer.putShort((short) majorVersion);
        outputBuffer.putShort((short) cpCount);

        for (int i = 1; i < cpCount; i++) {
            byte tag = tags[i];
            if (tag == 0) {
                continue;
            } else if (tag == ClassFile.CONSTANT_Utf8) {
                int len = Short.toUnsignedInt(buffer.position(positions[i]).getShort());

                byte[] mutf8 = new byte[len];
                buffer.get(mutf8);

                int index = context.getPool().addString(mutf8);

                outputBuffer.put(ClassFile.CONSTANT_EXTERNAL_Utf8);


            } else if (tag == ClassFile.CONSTANT_EXTERNAL_Descriptor) {
                // TODO
            } else {
                if (tag < 0) {
                    throw new AssertionError("Tag: " + tag);
                }
                outputBuffer.put(tag);
                outputBuffer.put(buffer.array(), arrayOffset + positions[i], ClassFile.CONSTANT_SIZE[tag]);
            }

        }

        // TODO

        throw new TODO();
    }
}
