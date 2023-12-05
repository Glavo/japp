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

import java.io.UTFDataFormatException;

public final class MUTF8 {
    public static int charsFromMUTF8Length(byte[] bytes, int offset, int count) {
        int length = 0;

        for (int i = offset; i < offset + count; i++) {
            byte ch = bytes[i];

            if (ch == 0) {
                break;
            }

            if ((ch & 0xC0) != 0x80) {
                length++;
            }
        }

        return length;
    }

    public static void charsFromMUTF8(char[] chars, byte[] bytes, int offset, int count) throws UTFDataFormatException {
        int j = 0;

        for (int i = offset; i < offset + count; i++) {
            byte ch = bytes[i];

            if (ch == 0) {
                break;
            }

            boolean is_unicode = (ch & 0x80) != 0;
            int uch = ch & 0x7F;

            if (is_unicode) {
                int mask = 0x40;

                while ((uch & mask) != 0) {
                    ch = bytes[++i];

                    if ((ch & 0xC0) != 0x80) {
                        throw new UTFDataFormatException("bad continuation 0x" + Integer.toHexString(ch));
                    }

                    uch = ((uch & ~mask) << 6) | (ch & 0x3F);
                    mask <<= 6 - 1;
                }

                if ((uch & 0xFFFF) != uch) {
                    throw new UTFDataFormatException("character out of range \\u" + Integer.toHexString(uch));
                }
            }

            chars[j++] = (char) uch;
        }
    }

    public static String stringFromMUTF8(byte[] bytes, int offset, int count) {
        int length = charsFromMUTF8Length(bytes, offset, count);
        char[] chars = new char[length];

        try {
            charsFromMUTF8(chars, bytes, offset, count);
        } catch (UTFDataFormatException ex) {
            throw new InternalError("Attempt to convert non modified UTF-8 byte sequence", ex);
        }

        return new String(chars);
    }

    public static String stringFromMUTF8(byte[] bytes) {
        return stringFromMUTF8(bytes, 0, bytes.length);
    }

}
