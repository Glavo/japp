/*
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

import jdk.internal.misc.Unsafe;

import java.nio.ByteOrder;

public final class UnsafeUtil {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    public static final int ARRAY_BYTE_BASE_OFFSET = Unsafe.ARRAY_BYTE_BASE_OFFSET;

    private UnsafeUtil() {
    }

    public static byte getByte(Object o, long offset) {
        return UNSAFE.getByte(o, offset);
    }

    public static int getUnsignedByte(Object o, long offset) {
        return Byte.toUnsignedInt(getByte(o, offset));
    }

    public static void putByte(Object o, long offset, byte x) {
        UNSAFE.putByte(o, offset, x);
    }

    public static short getShort(Object o, long offset) {
        short res = UNSAFE.getShort(o, offset);
        if (IS_BIG_ENDIAN) {
            res = Short.reverseBytes(res);
        }
        return res;
    }

    public static void putShort(Object o, long offset, short x) {
        UNSAFE.putShort(o, offset, IS_BIG_ENDIAN ? Short.reverseBytes(x) : x);
    }

    public static int getInt(Object o, long offset) {
        int res = UNSAFE.getInt(o, offset);
        if (IS_BIG_ENDIAN) {
            res = Integer.reverseBytes(res);
        }
        return res;
    }

    public static long getUnsignedInt(Object o, long offset) {
        return Integer.toUnsignedLong(getInt(o, offset));
    }

    public static void putInt(Object o, long offset, int x) {
        UNSAFE.putInt(o, offset, IS_BIG_ENDIAN ? Integer.reverseBytes(x) : x);
    }

    public static long getLong(Object o, long offset) {
        long res = UNSAFE.getLong(o, offset);
        if (IS_BIG_ENDIAN) {
            res = Long.reverseBytes(res);
        }
        return res;
    }

    public static void putLong(Object o, long offset, long x) {
        UNSAFE.putLong(o, offset, IS_BIG_ENDIAN ? Long.reverseBytes(x) : x);
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        UNSAFE.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    }
}
