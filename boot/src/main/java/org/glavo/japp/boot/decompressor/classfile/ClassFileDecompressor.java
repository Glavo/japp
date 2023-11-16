package org.glavo.japp.boot.decompressor.classfile;

import org.glavo.japp.TODO;
import org.glavo.japp.boot.JAppReader;
import org.glavo.japp.classfile.ClassFile;
import org.glavo.japp.util.CompressedNumber;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.glavo.japp.classfile.ClassFile.*;

public final class ClassFileDecompressor {
    public static byte[] decompress(JAppReader reader, ByteBuffer compressed, byte[] output) throws IOException {
        compressed.order(ByteOrder.BIG_ENDIAN);
        ByteBuffer outputBuffer = ByteBuffer.wrap(output);
        ByteArrayPool pool = reader.getPool();

        int magic = compressed.getInt();
        if (magic != ClassFile.MAGIC_NUMBER) {
            throw new IOException("Invalid magic number: " + Integer.toHexString(magic));
        }
        outputBuffer.putInt(magic);

        // minor version and major version
        outputBuffer.putInt(compressed.getInt());

        int constantPoolCount = Short.toUnsignedInt(compressed.getShort());
        outputBuffer.putShort((short) constantPoolCount);

        for (int i = 1; i < constantPoolCount; i++) {
            byte tag = compressed.get();

            if (tag > 0) {
                outputBuffer.put(tag);
            } else if (tag < 0) {
                outputBuffer.put(CONSTANT_Utf8);
            } else {
                throw new IOException();
            }

            switch (tag) {
                case CONSTANT_Utf8: {
                    int len = Short.toUnsignedInt(compressed.getShort());
                    outputBuffer.putShort((short) len);
                    outputBuffer.put(compressed.array(), compressed.arrayOffset() + compressed.position(), len);
                    compressed.position(compressed.position() + len);
                    break;
                }
                case CONSTANT_EXTERNAL_STRING: {
                    int index = CompressedNumber.getInt(compressed);

                    int position = outputBuffer.position();
                    outputBuffer.position(position + 2);
                    int len = pool.get(index, outputBuffer);
                    outputBuffer.putShort(position, (short) len);
                    break;
                }
                case CONSTANT_EXTERNAL_STRING_Descriptor: {
                    int index = CompressedNumber.getInt(compressed);
                    ByteBuffer compressedDescriptor = pool.get(index);

                    int lengthPosition = outputBuffer.position();
                    outputBuffer.position(lengthPosition + 2);

                    while (compressedDescriptor.hasRemaining()) {
                        byte b = compressedDescriptor.get();
                        outputBuffer.put(b);
                        if (b == 'L') {
                            int packageIndex = CompressedNumber.getInt(compressedDescriptor);
                            int classIndex = CompressedNumber.getInt(compressedDescriptor);

                            if (pool.get(packageIndex, outputBuffer) > 0) {
                                outputBuffer.put((byte) '/');
                            }

                            pool.get(classIndex, outputBuffer);
                        }
                    }

                    int len = outputBuffer.position() - lengthPosition - 2;
                    outputBuffer.putShort(lengthPosition, (short) len);
                    break;
                }
                default: {
                    byte size = CONSTANT_SIZE[tag];
                    if (size == 0) {
                        throw new IOException();
                    }

                    outputBuffer.put(compressed.array(), compressed.arrayOffset() + compressed.position(), size);
                }
            }
        }

        throw new TODO();
    }
}
