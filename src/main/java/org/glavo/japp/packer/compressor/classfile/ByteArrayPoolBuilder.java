package org.glavo.japp.packer.compressor.classfile;

import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.util.ByteBufferOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
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
        int size = map.size();
        int bytesSize = bytes.position();
        output.writeInt(size);
        output.writeInt(bytesSize);
        output.writeBytes(sizes.array(), 0, sizes.position());
        output.writeBytes(bytes.array(), 0, bytes.position());
    }

    public ByteArrayPool toPool() throws IOException {
        ByteBufferOutputStream output = new ByteBufferOutputStream();
        writeTo(output);
        return ByteArrayPool.readPool(Channels.newChannel(
                new ByteArrayInputStream(output.getByteBuffer().array(), 0, output.getTotalBytes())));
    }
}
