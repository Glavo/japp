package org.glavo.japp.boot.decompressor.classfile;

import org.glavo.japp.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public final class StringPool {
    private final byte[] bytes;
    private final long[] offsetAndSize;

    private StringPool(byte[] bytes, long[] offsetAndSize) {
        this.bytes = bytes;
        this.offsetAndSize = offsetAndSize;
    }

    public static StringPool readStringPool(FileChannel channel) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.readFully(channel, headerBuffer);

        int size = headerBuffer.getInt();
        int bytesSize = headerBuffer.getInt();

        byte[] bytes = new byte[bytesSize];
        ByteBuffer bytesBuffer = ByteBuffer.wrap(bytes);
        IOUtils.readFully(channel, bytesBuffer);

        long[] offsetAndSize = new long[size];

        bytesBuffer.flip();
        for (int i = 0; i < size; i++) {
            int o = bytesBuffer.position();
            int s = Short.toUnsignedInt(bytesBuffer.getShort());

            offsetAndSize[i] = (((long) s) << 32) | (long) o;
        }

        return new StringPool(bytes, offsetAndSize);
    }

    public int getMUTF8(int index, byte[] out, int outOffset) {
        long l = offsetAndSize[index];
        int offset = (int) (l & 0xffff_ffffL);
        int size = (int) (l >>> 32);

        System.arraycopy(bytes, offset, out, outOffset, size);
        return size;
    }
}
