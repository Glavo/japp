package org.glavo.japp.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Random;

public class CompressedNumberTest {

    private static void testCompressInt(int v) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        CompressedNumber.putInt(buffer, v);
        buffer.flip();
        int cv = CompressedNumber.getInt(buffer);

        Assertions.assertEquals(v, cv);
    }

    @Test
    void testCompressInt() {
        for (int i = 0; i <= 256; i++) {
            testCompressInt(i);
        }

        Random random = new Random(0);
        for (int i = 0; i < 256; i++) {
            testCompressInt(random.nextInt(Integer.MAX_VALUE));
        }

        testCompressInt(Integer.MAX_VALUE);

        Assertions.assertThrows(AssertionError.class, () -> testCompressInt(-1));
        Assertions.assertThrows(AssertionError.class, () -> testCompressInt(-Integer.MAX_VALUE));
    }
}
