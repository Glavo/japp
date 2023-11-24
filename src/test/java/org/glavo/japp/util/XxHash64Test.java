package org.glavo.japp.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XxHash64Test {
    private static IntStream testArguments() {
        return IntStream.concat(
                IntStream.rangeClosed(0, 32),
                IntStream.iterate(33, it -> it < 512, it -> it + 7)
        ).flatMap(it -> IntStream.of(it, it + 8192));
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void test(int length) {
        byte[] data = new byte[length];
        new Random(0).nextBytes(data);

        ByteBuffer nativeBuffer = ByteBuffer.allocateDirect(length);
        nativeBuffer.put(data);
        nativeBuffer.clear();

        long expected = org.lwjgl.util.xxhash.XXHash.XXH64(nativeBuffer, 0L);

        long actual = XxHash64.hash(0L, data, 0, data.length);
        assertEquals(expected, actual);
    }
}
