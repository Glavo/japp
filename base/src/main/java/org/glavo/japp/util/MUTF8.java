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

import java.nio.charset.StandardCharsets;

public final class MUTF8 {
    public static String stringFromMUTF8(byte[] bytes, int offset, int count) {
        final int end = offset + count;

        int i;
        for (i = offset; i < end; i++) {
            if ((bytes[i] & 0x80) != 0) {
                break;
            }
        }

        if (i == end) {
            return new String(bytes, offset, count, StandardCharsets.US_ASCII);
        }

        StringBuilder builder = new StringBuilder(count);

        for (i = offset; i < end; i++) {
            byte ch = bytes[i];

            if (ch == 0) {
                break;
            }

            boolean isUnicode = (ch & 0x80) != 0;
            int uch = ch & 0x7F;

            if (isUnicode) {
                int mask = 0x40;

                while ((uch & mask) != 0) {
                    ch = bytes[++i];

                    if ((ch & 0xC0) != 0x80) {
                        throw new IllegalArgumentException("bad continuation 0x" + Integer.toHexString(ch));
                    }

                    uch = ((uch & ~mask) << 6) | (ch & 0x3F);
                    mask <<= 6 - 1;
                }

                if ((uch & 0xFFFF) != uch) {
                    throw new IllegalArgumentException("character out of range \\u" + Integer.toHexString(uch));
                }
            }

            builder.appendCodePoint(uch);
        }

        return builder.toString();
    }

    public static String stringFromMUTF8(byte[] bytes) {
        return stringFromMUTF8(bytes, 0, bytes.length);
    }

}
