package org.glavo.japp.packer.compressor.classfile;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.packer.JAppPacker;
import org.glavo.japp.util.CompressedNumber;
import org.glavo.japp.packer.compressor.CompressResult;
import org.glavo.japp.packer.compressor.Compressor;
import org.glavo.japp.util.MUTF8;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.glavo.japp.classfile.ClassFile.*;

public final class ClassFileCompressor implements Compressor {

    public static final ClassFileCompressor INSTANCE = new ClassFileCompressor();

    private static int maxCompressedSize(int input) {
        return input + input / 128 + 16;
    }

    @Override
    public CompressResult compress(JAppPacker packer, byte[] source) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(source);

        ClassFileReader reader = new ClassFileReader(buffer);

        int arrayOffset = buffer.arrayOffset();
        byte[] output = new byte[maxCompressedSize(source.length)];
        ByteBuffer outputBuffer = ByteBuffer.wrap(output);

        outputBuffer.putInt(MAGIC_NUMBER);
        outputBuffer.putShort((short) reader.minorVersion);
        outputBuffer.putShort((short) reader.majorVersion);
        outputBuffer.putShort((short) reader.cpCount);

        for (int i = 1; i < reader.cpCount; i++) {
            byte tag = reader.tags[i];
            if (tag == 0) {
                continue;
            } else if (tag == CONSTANT_Utf8 || tag < 0) {
                buffer.position(reader.positions[i]);
                int len = Short.toUnsignedInt(buffer.getShort());

                byte[] mutf8 = new byte[len];
                buffer.get(mutf8);

                if (tag == CONSTANT_EXTERNAL_STRING_Class) {
                    putConstantClassName(packer, mutf8, outputBuffer);
                } else if (tag == CONSTANT_EXTERNAL_STRING_Descriptor) {
                    putConstantDescriptor(packer, mutf8, outputBuffer);
                } else {
                    // TODO: Class and Signature
                    putConstantUTF8(packer, mutf8, outputBuffer);
                }
            } else {
                outputBuffer.put(tag);
                outputBuffer.put(buffer.array(), arrayOffset + reader.positions[i], CONSTANT_SIZE[tag]);
            }
        }

        outputBuffer.put(buffer.array(), buffer.arrayOffset() + reader.tailPosition, reader.tailLen);
        return new CompressResult(CompressionMethod.CLASSFILE, output, 0, outputBuffer.position());
    }

    private static void putConstantUTF8(JAppPacker packer, byte[] mutf8, ByteBuffer outputBuffer) throws IOException {
        outputBuffer.put(CONSTANT_EXTERNAL_STRING);
        CompressedNumber.putInt(outputBuffer, packer.getPool().add(mutf8));
    }

    private static void writeClassName(JAppPacker packer, byte[] mutf8, int offset, int end, int lastSlash, ByteBuffer outputBuffer) {
        byte[] packageBytes;
        byte[] classNameBytes;

        if (lastSlash >= 0) {
            packageBytes = Arrays.copyOfRange(mutf8, offset, lastSlash);
            classNameBytes = Arrays.copyOfRange(mutf8, lastSlash + 1, end);
        } else {
            packageBytes = new byte[0];
            classNameBytes = Arrays.copyOfRange(mutf8, offset, end);
        }

        CompressedNumber.putInt(outputBuffer, packer.getPool().add(packageBytes));
        CompressedNumber.putInt(outputBuffer, packer.getPool().add(classNameBytes));
    }

    private static void putConstantDescriptor(JAppPacker packer, byte[] mutf8, ByteBuffer outputBuffer) throws IOException {
        int offset = 0;
        for (; offset < mutf8.length; offset++) {
            if (mutf8[offset] == 'L') {
                break;
            }
        }

        if (offset == mutf8.length) {
            putConstantUTF8(packer, mutf8, outputBuffer);
            return;
        }

        ByteBuffer descriptorBuffer = ByteBuffer.allocate(mutf8.length * 2);
        descriptorBuffer.put(mutf8, 0, offset);

        for (; offset < mutf8.length; offset++) {
            byte b = mutf8[offset];
            if (b == 'L') {
                int lastSlash = -1;
                int semicolon = -1;

                for (int offset2 = offset + 1; offset2 < mutf8.length; offset2++) {
                    byte b2 = mutf8[offset2];

                    if (b2 == '/') {
                        lastSlash = offset2;
                    } else if (b2 == ';') {
                        semicolon = offset2;
                        break;
                    }
                }

                if (semicolon < 0 || lastSlash == offset + 1) {
                    throw new IOException("Invalid descriptor: " + MUTF8.stringFromMUTF8(mutf8));
                }

                descriptorBuffer.put(b);
                writeClassName(packer, mutf8, offset + 1, semicolon, lastSlash, descriptorBuffer);
                offset = semicolon;
            } else {
                descriptorBuffer.put(b);
            }
        }

        int index = packer.getPool().add(Arrays.copyOf(descriptorBuffer.array(), descriptorBuffer.position()));
        outputBuffer.put(CONSTANT_EXTERNAL_STRING_Descriptor);
        CompressedNumber.putInt(outputBuffer, index);
    }

    private static void putConstantClassName(JAppPacker packer, byte[] mutf8, ByteBuffer outputBuffer) throws IOException {
        if (mutf8.length == 0) {
            throw new IOException("Class name is empty");
        }

        if (mutf8[0] == '[') {
            putConstantDescriptor(packer, mutf8, outputBuffer);
            return;
        }

        int lastSlash = mutf8.length - 1;
        for (; lastSlash >= 0; lastSlash--) {
            if (mutf8[lastSlash] == '/') {
                break;
            }
        }

        if (lastSlash == 0 || lastSlash == mutf8.length - 1) {
            throw new IOException("Invalid class name: " + MUTF8.stringFromMUTF8(mutf8));
        }

        outputBuffer.put(CONSTANT_EXTERNAL_STRING_Class);
        writeClassName(packer, mutf8, 0, mutf8.length, lastSlash, outputBuffer);
    }
}
