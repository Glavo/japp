package org.glavo.japp.packer.compressor.classfile;

import org.glavo.japp.util.MUTF8;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;

public final class StringPoolBuilder {
    private final HashMap<String, Integer> strings = new HashMap<>();
    private ByteBuffer bytes = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);

    private void growIfNeed(int s) {
        if (bytes.remaining() < s + 2) {
            int nextLen = Math.max(bytes.limit() * 2, bytes.position() + s + 2);
            bytes = ByteBuffer.wrap(Arrays.copyOf(bytes.array(), nextLen)).order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    public int addString(byte[] mutf8) {
        assert mutf8.length <= 0xffff;

        String str = MUTF8.stringFromMUTF8(mutf8);

        Integer index = strings.get(str);
        if (index != null) {
            return index;
        }

        index = strings.size();
        strings.put(str, index);

        growIfNeed(mutf8.length);
        bytes.putShort((short) mutf8.length);
        bytes.put(mutf8);

        return index;
    }

    public void writeTo(OutputStream out) throws IOException {
        int size = strings.size();
        int bytesSize = bytes.position();

        ByteBuffer headerBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        headerBuffer.putInt(size);
        headerBuffer.putInt(bytesSize);

        out.write(headerBuffer.array());
        out.write(bytes.array(), 0, bytes.position());
    }
}
