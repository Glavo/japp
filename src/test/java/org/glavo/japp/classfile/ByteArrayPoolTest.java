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
