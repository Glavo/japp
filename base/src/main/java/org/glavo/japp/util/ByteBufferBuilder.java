package org.glavo.japp.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class ByteBufferBuilder {
    private ByteBuffer buffer;

    public ByteBufferBuilder() {
        this(ByteOrder.LITTLE_ENDIAN, 8192);
    }

    public ByteBufferBuilder(ByteOrder order, int initialCapacity) {
        this.buffer = ByteBuffer.allocate(initialCapacity).order(order);
    }

    private void prepare(int next) {
        if (buffer.remaining() < next) {
            byte[] arr = buffer.array();
            int prevLen = arr.length;
            int nextLen = Math.max(prevLen * 2, prevLen + next);

            buffer = ByteBuffer.wrap(Arrays.copyOf(arr, nextLen)).order(buffer.order()).position(buffer.position());
        }
    }

    public void putByte(byte v) {
        prepare(Byte.BYTES);
        buffer.put(v);
    }

    public void putShort(short v) {
        prepare(Short.BYTES);
        buffer.putShort(v);
    }

    public void putUnsignedShort(int v) {
        if (v > 0xffff) {
            throw new IllegalArgumentException();
        }

        putShort((short) v);
    }

    public void putInt(int v) {
        prepare(Integer.BYTES);
        buffer.putInt(v);
    }

    public void putUnsignedInt(long v) {
        if (v > 0xffff_ffffL) {
            throw new IllegalArgumentException();
        }

        putInt((int) v);
    }

    public void putLong(long v) {
        prepare(Long.BYTES);
        buffer.putLong(v);
    }

    public void putBytes(byte[] array) {
        putBytes(array, 0, array.length);
    }

    public void putBytes(byte[] array, int offset, int len) {
        prepare(len);
        buffer.put(array, offset, len);
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buffer.array(), buffer.position());
    }
}
