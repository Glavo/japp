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
package org.glavo.japp.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class ByteBufferOutputStream extends OutputStream {
    private ByteBuffer buffer;

    public ByteBufferOutputStream() {
        this(ByteOrder.LITTLE_ENDIAN, 8192);
    }

    public ByteBufferOutputStream(int initialCapacity) {
        this(ByteOrder.LITTLE_ENDIAN, initialCapacity);
    }

    public ByteBufferOutputStream(ByteOrder order, int initialCapacity) {
        this.buffer = ByteBuffer.allocate(initialCapacity).order(order);
    }

    public ByteBufferOutputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    private void prepare(int next) {
        if (buffer.remaining() < next) {
            byte[] arr = buffer.array();
            int prevLen = buffer.position();
            int nextLen = Math.max(prevLen * 2, prevLen + next);

            buffer = ByteBuffer.allocate(nextLen).order(buffer.order());
            buffer.put(arr, 0, prevLen);
        }
    }

    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    public int getTotalBytes() {
        return buffer.position();
    }

    @Override
    public void write(int b) {
        writeByte((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        writeBytes(b, off, len);
    }

    public void writeByte(byte v) {
        prepare(Byte.BYTES);
        buffer.put(v);
    }

    public void writeShort(short v) {
        prepare(Short.BYTES);
        buffer.putShort(v);
    }

    public void writeUnsignedShort(int v) {
        if (v > 0xffff) {
            throw new IllegalArgumentException();
        }

        writeShort((short) v);
    }

    public void writeInt(int v) {
        prepare(Integer.BYTES);
        buffer.putInt(v);
    }

    public void writeUnsignedInt(long v) {
        if (v > 0xffff_ffffL) {
            throw new IllegalArgumentException();
        }

        writeInt((int) v);
    }

    public void writeLong(long v) {
        prepare(Long.BYTES);
        buffer.putLong(v);
    }

    public void writeBytes(byte[] array) {
        writeBytes(array, 0, array.length);
    }

    public void writeBytes(byte[] array, int offset, int len) {
        prepare(len);
        buffer.put(array, offset, len);
    }

    public void writeTo(OutputStream out) throws IOException {
        out.write(buffer.array(), 0, buffer.position());
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buffer.array(), buffer.position());
    }
}
