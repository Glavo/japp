/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.packer.compressor.classfile;

import com.github.luben.zstd.Zstd;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.boot.decompressor.zstd.ZstdFrameDecompressor;
import org.glavo.japp.util.ZstdUtils;
import org.glavo.japp.util.ByteBufferOutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;

public final class ByteArrayPoolBuilder {
    private static final class ByteArrayWrapper {
        final byte[] bytes;
        final int hash;

        private ByteArrayWrapper(byte[] bytes) {
            this.bytes = bytes;
            this.hash = Arrays.hashCode(bytes);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ByteArrayWrapper)) {
                return false;
            }

            ByteArrayWrapper other = (ByteArrayWrapper) obj;
            return Arrays.equals(this.bytes, other.bytes);
        }
    }

    private final HashMap<ByteArrayWrapper, Integer> map = new HashMap<>();
    private ByteBuffer bytes = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
    private ByteBuffer sizes = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);

    private void growIfNeed(int s) {
        if (bytes.remaining() < s) {
            int position = bytes.position();
            int nextLen = Math.max(bytes.limit() * 2, position + s);
            bytes = ByteBuffer.wrap(Arrays.copyOf(bytes.array(), nextLen)).position(position);
        }

        if (!sizes.hasRemaining()) {
            int position = sizes.position();
            sizes = ByteBuffer.wrap(Arrays.copyOf(sizes.array(), position * 2))
                    .position(position)
                    .order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    public int add(byte[] bytes) {
        assert bytes.length <= 0xffff;

        ByteArrayWrapper wrapper = new ByteArrayWrapper(bytes);

        Integer index = map.get(wrapper);
        if (index != null) {
            return index;
        }

        index = map.size();
        map.put(wrapper, index);

        growIfNeed(bytes.length);
        this.sizes.putShort((short) bytes.length);
        this.bytes.put(bytes);

        return index;
    }

    public void writeTo(ByteBufferOutputStream output) {
        int count = map.size();
        int uncompressedBytesSize = bytes.position();

        CompressionMethod compressionMethod;
        byte[] compressed = new byte[ZstdUtils.maxCompressedLength(uncompressedBytesSize)];
        long compressedBytesSize = Zstd.compressByteArray(compressed, 0, compressed.length, bytes.array(), 0, uncompressedBytesSize, 8);
        if (compressedBytesSize < uncompressedBytesSize) {
            compressionMethod = CompressionMethod.ZSTD;
        } else {
            compressionMethod = CompressionMethod.NONE;
            compressed = bytes.array();
            compressedBytesSize = uncompressedBytesSize;
        }

        output.writeByte(ByteArrayPool.MAGIC_NUMBER);
        output.writeByte(compressionMethod.id());
        output.writeShort((short) 0);
        output.writeInt(count);
        output.writeInt(uncompressedBytesSize);
        output.writeInt((int) compressedBytesSize);
        output.writeBytes(sizes.array(), 0, sizes.position());
        output.writeBytes(compressed, 0, (int) compressedBytesSize);
    }

    public ByteArrayPool toPool() throws IOException {
        ByteBufferOutputStream output = new ByteBufferOutputStream();
        writeTo(output);
        return ByteArrayPool.readFrom(output.getByteBuffer().flip(), new ZstdFrameDecompressor());
    }
}
