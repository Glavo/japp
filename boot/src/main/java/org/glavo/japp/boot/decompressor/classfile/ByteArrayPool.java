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
import org.glavo.japp.boot.decompressor.zstd.ZstdFrameDecompressor;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class ByteArrayPool {
    public static final byte MAGIC_NUMBER = (byte) 0xf0;

    private final byte[] bytes;
    private final long[] offsetAndSize;

    private ByteArrayPool(byte[] bytes, long[] offsetAndSize) {
        this.bytes = bytes;
        this.offsetAndSize = offsetAndSize;
    }

    public static ByteArrayPool readFrom(ByteBuffer buffer, ZstdFrameDecompressor decompressor) throws IOException {
        byte magic = buffer.get();
        if (magic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong boot magic: 0x%02x", Byte.toUnsignedInt(magic)));
        }

        CompressionMethod compressionMethod = CompressionMethod.readFrom(buffer);

        short reserved = buffer.getShort();
        if (reserved != 0) {
            throw new IOException("Reserved is not zero");
        }

        int count = buffer.getInt();
        int uncompressedBytesSize = buffer.getInt();
        int compressedBytesSize = buffer.getInt();

        long[] offsetAndSize = new long[count];

        int offset = 0;
        for (int i = 0; i < count; i++) {
            int s = Short.toUnsignedInt(buffer.getShort());
            offsetAndSize[i] = (((long) s) << 32) | (long) offset;
            offset += s;
        }

        byte[] compressedBytes = new byte[compressedBytesSize];
        buffer.get(compressedBytes);

        byte[] uncompressedBytes;
        if (compressionMethod == CompressionMethod.NONE) {
            uncompressedBytes = compressedBytes;
        } else {
            uncompressedBytes = new byte[uncompressedBytesSize];
            if (compressionMethod == CompressionMethod.ZSTD) {
                decompressor.decompress(compressedBytes, 0, compressedBytesSize, uncompressedBytes, 0, uncompressedBytesSize);
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
