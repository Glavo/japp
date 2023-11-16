package org.glavo.japp.boot.decompressor.classfile;

import org.glavo.japp.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

public final class ByteArrayPool {
    private final ByteBuffer bytes;
    private final long[] offsetAndSize;

    private ByteArrayPool(ByteBuffer bytes, long[] offsetAndSize) {
        this.bytes = bytes;
        this.offsetAndSize = offsetAndSize;
    }

    public static ByteArrayPool readPool(ReadableByteChannel channel) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.readFully(channel, headerBuffer);
        headerBuffer.flip();

        int size = headerBuffer.getInt();
        int bytesSize = headerBuffer.getInt();

        long[] offsetAndSize = new long[size];

        int offset = 0;
        ByteBuffer sizes = ByteBuffer.allocate(size * 2).order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.readFully(channel, sizes);
        sizes.flip();
        for (int i = 0; i < size; i++) {
            int s = Short.toUnsignedInt(sizes.getShort());
            offsetAndSize[i] = (((long) s) << 32) | (long) offset;
            offset += s;
        }

        ByteBuffer bytes = ByteBuffer.allocate(bytesSize);
        IOUtils.readFully(channel, bytes);
        bytes.flip();

        return new ByteArrayPool(bytes, offsetAndSize);
    }

    public ByteBuffer get(int index) {
        long l = offsetAndSize[index];
        int offset = (int) (l & 0xffff_ffffL);
        int size = (int) (l >>> 32);

        return bytes.slice().limit(offset + size).position(offset);
    }

    public int get(int index, byte[] out, int outOffset) {
        long l = offsetAndSize[index];
        int offset = (int) (l & 0xffff_ffffL);
        int size = (int) (l >>> 32);

        bytes.slice().limit(offset + size).position(offset).get(out, outOffset, size);
        return size;
    }

    public int get(int index, ByteBuffer output) {
        int len = get(index, output.array(), output.arrayOffset() + output.position());
        output.position(output.position() + len);
        return len;
    }
}
