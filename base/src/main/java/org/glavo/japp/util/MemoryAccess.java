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

import jdk.internal.misc.Unsafe;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public final class MemoryAccess {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final long BUFFER_ADDRESS_OFFSET = UNSAFE.objectFieldOffset(Buffer.class, "address");

    public static final int ARRAY_BYTE_BASE_OFFSET = Unsafe.ARRAY_BYTE_BASE_OFFSET;

    private MemoryAccess() {
    }

    public static long getDirectBufferAddress(ByteBuffer buffer) {
        assert buffer.isDirect();
        return UNSAFE.getLong(buffer, BUFFER_ADDRESS_OFFSET);
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
        return UNSAFE.getShortUnaligned(o, offset, false);
    }

    public static void putShort(Object o, long offset, short x) {
        UNSAFE.putShortUnaligned(o, offset, x, false);
    }

    public static int getInt(Object o, long offset) {
        return UNSAFE.getIntUnaligned(o, offset, false);
    }

    public static long getUnsignedInt(Object o, long offset) {
        return Integer.toUnsignedLong(getInt(o, offset));
    }

    public static void putInt(Object o, long offset, int x) {
        UNSAFE.putIntUnaligned(o, offset, x, false);
    }

    public static long getLong(Object o, long offset) {
        return UNSAFE.getLongUnaligned(o, offset, false);
    }

    public static void putLong(Object o, long offset, long x) {
        UNSAFE.putLongUnaligned(o, offset, x, false);
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        UNSAFE.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    }
}
