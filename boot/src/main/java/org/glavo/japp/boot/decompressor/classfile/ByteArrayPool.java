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
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
import org.glavo.japp.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

public final class ByteArrayPool {
    public static final byte MAGIC_NUMBER = (byte) 0xf0;

    private final byte[] bytes;
    private final long[] offsetAndSize;

    private ByteArrayPool(byte[] bytes, long[] offsetAndSize) {
        this.bytes = bytes;
        this.offsetAndSize = offsetAndSize;
    }

    public static ByteArrayPool readFrom(ReadableByteChannel channel) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.readFully(channel, headerBuffer);
        headerBuffer.flip();

        byte magic = headerBuffer.get();
        if (magic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong boot magic: %02x", Byte.toUnsignedInt(magic)));
        }

        CompressionMethod compressionMethod = CompressionMethod.readFrom(headerBuffer);

        short reserved = headerBuffer.getShort();
        if (reserved != 0) {
            throw new IOException("Reserved is not zero");
        }

        int count = headerBuffer.getInt();
        int uncompressedBytesSize = headerBuffer.getInt();
        int compressedBytesSize = headerBuffer.getInt();

        long[] offsetAndSize = new long[count];

        int offset = 0;
        ByteBuffer sizes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.readFully(channel, sizes);
        sizes.flip();
        for (int i = 0; i < count; i++) {
            int s = Short.toUnsignedInt(sizes.getShort());
            offsetAndSize[i] = (((long) s) << 32) | (long) offset;
            offset += s;
        }

        byte[] compressedBytes = new byte[compressedBytesSize];
        IOUtils.readFully(channel, ByteBuffer.wrap(compressedBytes));

        byte[] uncompressedBytes;
        if (compressionMethod == CompressionMethod.NONE) {
            uncompressedBytes = compressedBytes;
        } else {
            uncompressedBytes = new byte[uncompressedBytesSize];
            if (compressionMethod == CompressionMethod.ZSTD) {
                ZstdUtils.decompress(compressedBytes, 0, compressedBytesSize, uncompressedBytes, 0, uncompressedBytesSize);
            } else {
                throw new IOException("Unsupported compression method: " + compressionMethod);
            }
        }

        return new ByteArrayPool(uncompressedBytes, offsetAndSize);
    }

    public ByteBuffer get(int index) {
        long l = offsetAndSize[index];
        int offset = (int) (l & 0xffff_ffffL);
        int size = (int) (l >>> 32);

        return ByteBuffer.wrap(bytes, offset, size);
    }

    public int get(int index, ByteBuffer output) {
        long l = offsetAndSize[index];
        int offset = (int) (l & 0xffff_ffffL);
        int size = (int) (l >>> 32);

        output.put(bytes, offset, size);
        return size;
    }
}
