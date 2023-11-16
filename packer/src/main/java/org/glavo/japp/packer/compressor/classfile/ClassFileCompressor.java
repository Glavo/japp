package org.glavo.japp.packer.compressor.classfile;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.classfile.ClassFile;
import org.glavo.japp.util.CompressedNumber;
import org.glavo.japp.packer.compressor.CompressContext;
import org.glavo.japp.packer.compressor.CompressResult;
import org.glavo.japp.packer.compressor.Compressor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class ClassFileCompressor implements Compressor {

    public static final ClassFileCompressor INSTANCE = new ClassFileCompressor();

    private static int maxCompressedSize(int input) {
        return input + input / 128 + 16;
    }

    @Override
    public CompressResult compress(CompressContext context, byte[] source) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(source);

        ClassFileReader reader = new ClassFileReader(buffer);

        int arrayOffset = buffer.arrayOffset();
        byte[] output = new byte[maxCompressedSize(source.length)];
        ByteBuffer outputBuffer = ByteBuffer.wrap(output);

        outputBuffer.putInt(ClassFile.MAGIC_NUMBER);
        outputBuffer.putShort((short) reader.minorVersion);
        outputBuffer.putShort((short) reader.majorVersion);
        outputBuffer.putShort((short) reader.cpCount);

        for (int i = 1; i < reader.cpCount; i++) {
            byte tag = reader.tags[i];
            if (tag == 0) {
                continue;
            } else if (tag == ClassFile.CONSTANT_Utf8 || tag < 0) {
                buffer.position(reader.positions[i]);
                int len = Short.toUnsignedInt(buffer.getShort());

                byte[] mutf8 = new byte[len];
                buffer.get(mutf8);

                if (tag == ClassFile.CONSTANT_EXTERNAL_STRING_Descriptor) {
                    putConstantDescriptor(context, mutf8, outputBuffer);
                } else {
                    // TODO: Class and Signature
                    putConstantUTF8(context, mutf8, outputBuffer);
                }
            } else {
                outputBuffer.put(tag);
                outputBuffer.put(buffer.array(), arrayOffset + reader.positions[i], ClassFile.CONSTANT_SIZE[tag]);
            }

        }

        outputBuffer.put(buffer.array(), buffer.arrayOffset() + reader.tailPosition, reader.tailLen);
        return new CompressResult(CompressionMethod.CLASSFILE, output, 0, outputBuffer.position());
    }

    private static void putConstantUTF8(CompressContext context, byte[] mutf8, ByteBuffer outputBuffer) throws IOException {
        outputBuffer.put(ClassFile.CONSTANT_EXTERNAL_STRING);
        CompressedNumber.putInt(outputBuffer, context.getPool().add(mutf8));
    }

    private static void putConstantDescriptor(CompressContext context, byte[] mutf8, ByteBuffer outputBuffer) throws IOException {
        int offset = 0;
        for (; offset < mutf8.length; offset++) {
            if (mutf8[offset] == 'L') {
                break;
            }
        }

        if (offset == mutf8.length) {
            putConstantUTF8(context, mutf8, outputBuffer);
            return;
        }


        ByteBuffer descriptorBuffer = ByteBuffer.allocate(mutf8.length * 2);
        descriptorBuffer.put(mutf8, 0, offset);

        for (; offset < mutf8.length; offset++) {
            byte b = mutf8[offset];
            if (b == 'L') {
                int lastSlash = 0;
                int semicolon = 0;

                for (int offset2 = offset + 1; offset2 < mutf8.length; offset2++) {
                    byte b2 = mutf8[offset2];

                    if (b2 == '/') {
                        lastSlash = offset2;
                    } else if (b2 == ';') {
                        semicolon = offset2;
                        break;
                    }
                }

                if (semicolon == 0 || lastSlash == offset + 1) {
                    throw new IOException("Invalid descriptor");
                }

                byte[] packageBytes;
                byte[] classNameBytes;

                if (lastSlash != 0) {
                    packageBytes = Arrays.copyOfRange(mutf8, offset + 1, lastSlash);
                    classNameBytes = Arrays.copyOfRange(mutf8, lastSlash + 1, semicolon);
                } else {
                    packageBytes = new byte[0];
                    classNameBytes = Arrays.copyOfRange(mutf8, offset + 1, semicolon);
                }

                descriptorBuffer.put(b);
                CompressedNumber.putInt(descriptorBuffer, context.getPool().add(packageBytes));
                CompressedNumber.putInt(descriptorBuffer, context.getPool().add(classNameBytes));
                offset = semicolon + 1;
            } else {
                descriptorBuffer.put(b);
            }
        }

        int index = context.getPool().add(Arrays.copyOf(descriptorBuffer.array(), descriptorBuffer.position()));
        outputBuffer.put(ClassFile.CONSTANT_EXTERNAL_STRING_Descriptor);
        CompressedNumber.putInt(outputBuffer, index);
    }
}
