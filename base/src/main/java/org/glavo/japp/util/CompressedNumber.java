/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.util;

import java.nio.ByteBuffer;

public final class CompressedNumber {

    private static final int SIGN_MASK = 0b1000_0000;
    private static final int VALUE_MASK = 0b0111_1111;
    private static final int TAIL_VALUE_MASK = 0b0111;

    public static void putInt(ByteBuffer out, int value) {
        if (value < 0) {
            throw new AssertionError();
        }
        do {
            int b = value & VALUE_MASK;
            value >>>= 7;

            if (value != 0) {
                b |= SIGN_MASK;
            }

            out.put((byte) b);
        } while (value != 0);
    }

    public static int getInt(ByteBuffer in) {
        int res = 0;
        for (int i = 0; i < 4; i++) {
            int b = Byte.toUnsignedInt(in.get());
            int bv = b & VALUE_MASK;

            res = res | (bv << (7 * i));
            if (b == bv) {
                return res;
            }
        }

        int b = Byte.toUnsignedInt(in.get());
        int bv = b & TAIL_VALUE_MASK;
        if (b != bv) {
            throw new AssertionError();
        }

        return res | (bv << (7 * 4));
    }

    private CompressedNumber() {
    }
}
