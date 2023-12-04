/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.boot.decompressor.classfile;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.JAppReader;
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
import org.glavo.japp.classfile.ClassFile;
import org.glavo.japp.util.CompressedNumber;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.glavo.japp.classfile.ClassFile.*;

public final class ClassFileDecompressor {
    public static void decompress(JAppReader reader, ByteBuffer compressed, byte[] output) throws IOException {
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
                throw new IOException("tag is 0");
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
                case CONSTANT_EXTERNAL_STRING_Class: {
                    int packageIndex = CompressedNumber.getInt(compressed);
                    int classIndex = CompressedNumber.getInt(compressed);

                    int position = outputBuffer.position();
                    outputBuffer.position(position + 2);
                    if (pool.get(packageIndex, outputBuffer) > 0) {
                        outputBuffer.put((byte) '/');
                    }

                    pool.get(classIndex, outputBuffer);

                    outputBuffer.putShort(position, (short) (outputBuffer.position() - position - 2));
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
                            outputBuffer.put((byte) ';');
                        }
                    }

                    int len = outputBuffer.position() - lengthPosition - 2;
                    outputBuffer.putShort(lengthPosition, (short) len);
                    break;
                }
                case CONSTANT_EXTERNAL_STRING_Signature: {
                    int index = CompressedNumber.getInt(compressed);
                    ByteBuffer compressedSignature = pool.get(index);

                    int lengthPosition = outputBuffer.position();
                    outputBuffer.position(lengthPosition + 2);

                    while (compressedSignature.hasRemaining()) {
                        byte b = compressedSignature.get();
                        outputBuffer.put(b);
                        if (b == 'L') {
                            int p = outputBuffer.position();
                            int packageIndex = CompressedNumber.getInt(compressedSignature);
                            int classIndex = CompressedNumber.getInt(compressedSignature);

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
                    if (tag == CONSTANT_Long || tag == CONSTANT_Double) {
                        i++;
                    }

                    byte size = CONSTANT_SIZE[tag];
                    if (size == 0) {
                        throw new IOException(String.format("Unknown tag: 0x%02x", Byte.toUnsignedInt(tag)));
                    }

                    int compressedLimit = compressed.limit();
                    outputBuffer.put(compressed.limit(compressed.position() + size));
                    compressed.limit(compressedLimit);
                }
            }
        }

        CompressionMethod compressionMethod = CompressionMethod.readFrom(compressed);
        if (compressionMethod == CompressionMethod.NONE) {
            if (compressed.remaining() != outputBuffer.remaining()) {
                throw new IOException(String.format("The remaining bytes do not match: %d != %d",
                        compressed.remaining(), outputBuffer.remaining()));
            }

            outputBuffer.put(compressed);
        } else if (compressionMethod == CompressionMethod.ZSTD) {
            ZstdUtils.decompress(compressed, outputBuffer);
            if (compressed.hasRemaining() || outputBuffer.hasRemaining()) {
                throw new IOException();
            }
        } else {
            throw new IOException("Unsupported compression method: " + compressionMethod);
        }
    }
}
