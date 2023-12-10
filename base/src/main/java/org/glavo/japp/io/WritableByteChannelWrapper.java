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
package org.glavo.japp.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

final class WritableByteChannelWrapper implements LittleEndianDataOutput {
    private final WritableByteChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
    private long totalBytes = 0L;

    WritableByteChannelWrapper(OutputStream outputStream) {
        this.channel = Channels.newChannel(outputStream);
    }

    WritableByteChannelWrapper(WritableByteChannel channel) {
        this.channel = channel;
    }

    private void flushBuffer() throws IOException {
        if (buffer.position() > 0) {
            buffer.flip();
            IOUtils.writeFully(channel, buffer);
            buffer.clear();
        }
    }

    private void prepare(int next) throws IOException {
        if (buffer.remaining() < next) {
            flushBuffer();
        }
    }

    @Override
    public long getTotalBytes() {
        return totalBytes;
    }

    @Override
    public void writeByte(byte v) throws IOException {
        prepare(Byte.BYTES);
        buffer.put(v);
        totalBytes += Byte.BYTES;
    }

    @Override
    public void writeShort(short v) throws IOException {
        prepare(Short.BYTES);
        buffer.putShort(v);
        totalBytes += Short.BYTES;
    }

    @Override
    public void writeInt(int v) throws IOException {
        prepare(Integer.BYTES);
        buffer.putInt(v);
        totalBytes += Integer.BYTES;
    }

    @Override
    public void writeLong(long v) throws IOException {
        prepare(Long.BYTES);
        buffer.putLong(v);
        totalBytes += Long.BYTES;
    }

    @Override
    public void writeBytes(byte[] array, int offset, int len) throws IOException {
        Objects.checkFromIndexSize(offset, len, array.length);
        if (len == 0) {
            return;
        }

        if (len < buffer.capacity()) {
            if (len > buffer.remaining()) {
                flushBuffer();
            }
            buffer.put(array, offset, len);
        } else {
            flushBuffer();
            IOUtils.writeFully(channel, ByteBuffer.wrap(array, offset, len));
        }
        totalBytes += len;
    }

    @Override
    public void close() throws IOException {
        try {
            flushBuffer();
        } finally {
            channel.close();
        }
    }
}
