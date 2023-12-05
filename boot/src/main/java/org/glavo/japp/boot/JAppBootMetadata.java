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
package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.boot.decompressor.zstd.ZstdFrameDecompressor;
import org.glavo.japp.util.ByteBufferUtils;
import org.glavo.japp.util.XxHash64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public final class JAppBootMetadata {
    public static final int MAGIC_NUMBER = 0x544f4f42;

    public static JAppBootMetadata readFrom(ByteBuffer buffer, ZstdFrameDecompressor decompressor) throws IOException {
        int bootMagic = buffer.getInt();
        if (bootMagic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong boot magic: 0x%02x", bootMagic));
        }

        int groupCount = buffer.getInt();

        ByteArrayPool pool = ByteArrayPool.readFrom(buffer, decompressor);

        JAppResourceGroup[] groups = new JAppResourceGroup[groupCount];
        {
            // ByteBuffer compressedBuffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
            ByteBuffer uncompressedBuffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < groupCount; i++) {
                byte magic = buffer.get();
                if (magic != JAppResourceGroup.MAGIC_NUMBER) {
                    throw new IOException(String.format("Wrong resource group magic: 0x%02x", magic));
                }

                CompressionMethod compressionMethod = CompressionMethod.readFrom(buffer);

                short reserved = buffer.getShort();
                if (reserved != 0) {
                    throw new IOException("Reserved is not 0");
                }

                int uncompressedSize = buffer.getInt();
                int compressedSize = buffer.getInt();
                int resourcesCount = buffer.getInt();
                long checksum = buffer.getLong();

                JAppResourceGroup group = new JAppResourceGroup();

                ByteBuffer compressedBuffer = ByteBufferUtils.slice(buffer, buffer.position(), compressedSize).order(ByteOrder.LITTLE_ENDIAN);
                buffer.position(buffer.position() + compressedSize);

                ByteBuffer uncompressed;
                if (compressionMethod == CompressionMethod.NONE) {
                    uncompressed = compressedBuffer;
                } else {
                    if (uncompressedBuffer.capacity() >= uncompressedSize) {
                        uncompressedBuffer.clear();
                    } else {
                        int nextLen = Math.max(uncompressedSize, uncompressedBuffer.capacity() * 2);
                        uncompressedBuffer = ByteBuffer.allocate(nextLen).order(ByteOrder.LITTLE_ENDIAN);
                    }
                    uncompressedBuffer.limit(uncompressedSize);

                    if (compressionMethod == CompressionMethod.ZSTD) {
                        decompressor.decompress(compressedBuffer, uncompressedBuffer);
                        if (uncompressedBuffer.hasRemaining()) {
                            throw new IOException();
                        }
                        uncompressedBuffer.flip();
                    } else {
                        throw new IOException("Unsupported compression method: " + compressionMethod);
                    }
                    uncompressed = uncompressedBuffer;
                }

                long actualChecksum = XxHash64.hashByteBufferWithoutUpdate(uncompressed);
                if (actualChecksum != checksum) {
                    throw new IOException(String.format(
                            "Failed while verifying resource group at index %d (expected=%x, actual=%x)",
                            i, checksum, actualChecksum
                    ));
                }

                for (int j = 0; j < resourcesCount; j++) {
                    JAppResource resource = JAppResource.readFrom(uncompressed);
                    group.put(resource.getName(), resource);
                }

                if (uncompressedBuffer.hasRemaining()) {
                    throw new IOException();
                }

                groups[i] = group;
            }
        }

        return new JAppBootMetadata(Arrays.asList(groups), pool);
    }

    private final List<JAppResourceGroup> groups;
    private final ByteArrayPool pool;

    public JAppBootMetadata(List<JAppResourceGroup> groups, ByteArrayPool pool) {
        this.groups = groups;
        this.pool = pool;
    }

    public List<JAppResourceGroup> getGroups() {
        return groups;
    }

    public ByteArrayPool getPool() {
        return pool;
    }
}
