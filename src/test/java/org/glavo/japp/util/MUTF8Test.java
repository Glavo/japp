/*
 * Copyright (C) 2024 Glavo
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class MUTF8Test {
    private static Stream<String> strs() {
        return Stream.of(
                "",
                "\0\0\0",
                "Hello World!",
                "Hello\0测试字符串"
        );
    }

    @ParameterizedTest
    @MethodSource("strs")
    public void testDecode(String str) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DataOutputStream dataOutput = new DataOutputStream(byteArrayOutputStream)) {
            dataOutput.writeUTF(str);
        }

        byte[] arr = byteArrayOutputStream.toByteArray();

        assertEquals(str, MUTF8.stringFromMUTF8(Arrays.copyOfRange(arr, 2, arr.length)));
    }
}
