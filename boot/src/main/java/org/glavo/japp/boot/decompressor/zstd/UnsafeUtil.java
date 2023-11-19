package org.glavo.japp.boot.decompressor.zstd;

import jdk.internal.misc.Unsafe;

import java.nio.ByteOrder;

final class UnsafeUtil {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    public static final int ARRAY_BYTE_BASE_OFFSET = Unsafe.ARRAY_BYTE_BASE_OFFSET;

    private UnsafeUtil() {
    }

    public static byte getByte(Object o, long offset) {
        return UNSAFE.getByte(o, offset);
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
