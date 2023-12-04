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
