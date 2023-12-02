package org.glavo.japp.classfile;

import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.packer.compressor.classfile.ByteArrayPoolBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteArrayPoolTest {

    private static byte[] testString(int index) {
        return ("str" + index).getBytes(US_ASCII);
    }

    void test(int n) throws IOException {
        ByteArrayPoolBuilder builder = new ByteArrayPoolBuilder();

        for (int i = 0; i < n; i++) {
            assertEquals(i, builder.add(testString(i)));
            assertEquals(i, builder.add(testString(i)));
        }

        ByteArrayPool pool = builder.toPool();

        for (int i = 0; i < n; i++) {
            byte[] testString = testString(i);

            ByteBuffer buffer = ByteBuffer.allocate(20);
            assertEquals(testString.length, pool.get(i, buffer));
            assertEquals(testString.length, buffer.position());

            byte[] out = new byte[testString.length];
            buffer.flip().get(out);
            assertArrayEquals(testString, out);

            Arrays.fill(out, (byte) 0);
            pool.get(i).get(out);
            assertArrayEquals(testString, out);
        }
    }

    @Test
    void test() throws IOException {
        test(0);
        test(10);
        test(100);
    }
}
