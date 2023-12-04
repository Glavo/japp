/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        nativeBuffer.position(0);

        long expected = org.lwjgl.util.xxhash.XXHash.XXH64(nativeBuffer, 0L);
        nativeBuffer.position(0);

        assertEquals(expected, XxHash64.hashByteBufferWithoutUpdate(ByteBuffer.wrap(data)));
        assertEquals(expected, XxHash64.hashByteBufferWithoutUpdate(nativeBuffer));
    }
}
