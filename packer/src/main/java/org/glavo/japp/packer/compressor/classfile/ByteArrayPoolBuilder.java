package org.glavo.japp.packer.compressor.classfile;

import org.glavo.japp.util.CompressedNumber;

import java.io.IOException;
import java.io.OutputStream;
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

    private void growIfNeed(int s) {
        if (bytes.remaining() < s + 8) {
            int nextLen = Math.max(bytes.limit() * 2, bytes.position() + s + 8);
            bytes = ByteBuffer.wrap(Arrays.copyOf(bytes.array(), nextLen)).order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    public int add(byte[] bytes) {
        ByteArrayWrapper wrapper = new ByteArrayWrapper(bytes);

        Integer index = map.get(wrapper);
        if (index != null) {
            return index;
        }

        index = map.size();
        map.put(wrapper, index);

        growIfNeed(bytes.length);
        CompressedNumber.putInt(this.bytes, bytes.length);
        this.bytes.put(bytes);

        return index;
    }

    public void writeTo(OutputStream out) throws IOException {
        int size = map.size();
        int bytesSize = bytes.position();

        ByteBuffer headerBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        headerBuffer.putInt(size);
        headerBuffer.putInt(bytesSize);

        out.write(headerBuffer.array());
        out.write(bytes.array(), 0, bytes.position());
    }
}
