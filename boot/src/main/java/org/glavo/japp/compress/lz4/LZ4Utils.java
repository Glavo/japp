package org.glavo.japp.compress.lz4;

/*
 * Copyright 2020 Adrien Grand and the lz4-java contributors.
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.glavo.japp.compress.lz4.LZ4Constants.*;

final class LZ4Utils {
    private LZ4Utils() {
    }

    public static void checkRange(byte[] buf, int off) {
        if (off < 0 || off >= buf.length) {
            throw new ArrayIndexOutOfBoundsException(off);
        }
    }

    public static void checkRange(byte[] buf, int off, int len) {
        checkLength(len);
        if (len > 0) {
            checkRange(buf, off);
            checkRange(buf, off + len - 1);
        }
    }

    public static void checkLength(int len) {
        if (len < 0) {
            throw new IllegalArgumentException("lengths must be >= 0");
        }
    }

    private static final boolean USE_VAR_HANDLE = true;
    private static final VarHandle SHORT_ARRAY = MethodHandles.byteArrayViewVarHandle(short[].class, LITTLE_ENDIAN);
    private static final VarHandle INT_ARRAY = MethodHandles.byteArrayViewVarHandle(int[].class, LITTLE_ENDIAN);
    private static final VarHandle LONG_ARRAY = MethodHandles.byteArrayViewVarHandle(long[].class, LITTLE_ENDIAN);


    public static byte readByte(byte[] src, int srcOff) {
        return src[srcOff];
    }

    public static void writeByte(byte[] dest, int destOff, byte value) {
        dest[destOff] = value;
    }

    public static void writeByte(byte[] dest, int off, int i) {
        dest[off] = (byte) i;
    }

    public static short readShort(byte[] src, int srcOff) {
        if (USE_VAR_HANDLE) {
            return (short) SHORT_ARRAY.get(src, srcOff);
        }
        return (short) ((src[srcOff] & 0xFF) + (src[srcOff + 1] << 8));
    }

    public static int readShort(short[] src, int srcOff) {
        return src[srcOff];
    }

    public static void writeShort(byte[] dest, int destOffset, short value) {
        if (USE_VAR_HANDLE) {
            SHORT_ARRAY.set(dest, destOffset, (short) value);
            return;
        }
        dest[destOffset] = (byte) value;
        dest[destOffset + 1] = (byte) (value >>> 8);
    }

    public static void writeShort(byte[] dest, int destOffset, int value) {
        writeShort(dest, destOffset, (short) value);
    }

    public static void writeShort(short[] dest, int destOff, int value) {
        dest[destOff] = (short) value;
    }

    public static int readInt(byte[] src, int srcOff) {
        if (USE_VAR_HANDLE) {
            return (int) INT_ARRAY.get(src, srcOff);
        }

        return (src[srcOff] & 0xFF) | ((src[srcOff + 1] & 0xFF) << 8) | ((src[srcOff + 2] & 0xFF) << 16) | ((src[srcOff + 3] & 0xFF) << 24);
    }

    public static int readInt(int[] src, int srcOff) {
        return src[srcOff];
    }

    public static void writeInt(byte[] dest, int destOff, int value) {
        if (USE_VAR_HANDLE) {
            INT_ARRAY.set(dest, destOff, value);
            return;
        }
        dest[destOff] = (byte) (value);
        dest[destOff + 1] = (byte) (value >>> 8);
        dest[destOff + 2] = (byte) (value >>> 16);
        dest[destOff + 3] = (byte) (value >>> 24);
    }

    public static void writeInt(int[] dest, int destOff, int value) {
        dest[destOff] = value;
    }

    public static long readLong(byte[] src, int srcOff) {
        if (USE_VAR_HANDLE) {
            return (long) LONG_ARRAY.get(src, srcOff);
        }

        return (src[srcOff] & 0xFFL)
               + ((src[srcOff + 1] & 0xFFL) << 8)
               + ((src[srcOff + 2] & 0xFFL) << 16)
               + ((src[srcOff + 3] & 0xFFL) << 24)
               + ((src[srcOff + 4] & 0xFFL) << 32)
               + ((src[srcOff + 5] & 0xFFL) << 40)
               + ((src[srcOff + 6] & 0xFFL) << 48)
               + ((src[srcOff + 7] & 0xFFL) << 56);
    }

    public static void writeLong(byte[] dest, int destOff, long value) {
        if (USE_VAR_HANDLE) {
            LONG_ARRAY.set(dest, destOff, value);
            return;
        }
        dest[destOff] = (byte) (value);
        dest[destOff + 1] = (byte) (value >>> 8);
        dest[destOff + 2] = (byte) (value >>> 16);
        dest[destOff + 3] = (byte) (value >>> 24);
        dest[destOff + 4] = (byte) (value >>> 32);
        dest[destOff + 5] = (byte) (value >>> 40);
        dest[destOff + 6] = (byte) (value >>> 48);
        dest[destOff + 7] = (byte) (value >>> 56);
    }

    private static final int MAX_INPUT_SIZE = 0x7E000000;

    static int maxCompressedLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0, got " + length);
        } else if (length >= MAX_INPUT_SIZE) {
            throw new IllegalArgumentException("length must be < " + MAX_INPUT_SIZE);
        }
        return length + length / 255 + 16;
    }

    static int hash(int i) {
        return (i * -1640531535) >>> ((MIN_MATCH * 8) - HASH_LOG);
    }

    static int hash64k(int i) {
        return (i * -1640531535) >>> ((MIN_MATCH * 8) - HASH_LOG_64K);
    }

    static int hashHC(int i) {
        return (i * -1640531535) >>> ((MIN_MATCH * 8) - HASH_LOG_HC);
    }

    static void safeArraycopy(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
        final int fastLen = len & 0xFFFFFFF8;
        wildArraycopy(src, srcOff, dest, destOff, fastLen);
        for (int i = 0, slowLen = len & 0x7; i < slowLen; i += 1) {
            writeByte(dest, destOff + fastLen + i, readByte(src, srcOff + fastLen + i));
        }
    }

    static void wildArraycopy(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
        for (int i = 0; i < len; i += 8) {
            writeLong(dest, destOff + i, readLong(src, srcOff + i));
        }
    }

    static void wildIncrementalCopy(byte[] dest, int matchOff, int dOff, int matchCopyEnd) {
        if (dOff - matchOff < 4) {
            for (int i = 0; i < 4; ++i) {
                writeByte(dest, dOff + i, readByte(dest, matchOff + i));
            }
            dOff += 4;
            matchOff += 4;
            int dec = 0;
            assert dOff >= matchOff && dOff - matchOff < 8;
            switch (dOff - matchOff) {
                case 1:
                    matchOff -= 3;
                    break;
                case 2:
                    matchOff -= 2;
                    break;
                case 3:
                    matchOff -= 3;
                    dec = -1;
                    break;
                case 5:
                    dec = 1;
                    break;
                case 6:
                    dec = 2;
                    break;
                case 7:
                    dec = 3;
                    break;
                default:
                    break;
            }
            writeInt(dest, dOff, readInt(dest, matchOff));
            dOff += 4;
            matchOff -= dec;
        } else if (dOff - matchOff < COPY_LENGTH) {
            writeLong(dest, dOff, readLong(dest, matchOff));
            dOff += dOff - matchOff;
        }
        while (dOff < matchCopyEnd) {
            writeLong(dest, dOff, readLong(dest, matchOff));
            dOff += 8;
            matchOff += 8;
        }
    }

    static void safeIncrementalCopy(byte[] dest, int matchOff, int dOff, int matchLen) {
        for (int i = 0; i < matchLen; ++i) {
            dest[dOff + i] = dest[matchOff + i];
            writeByte(dest, dOff + i, readByte(dest, matchOff + i));
        }
    }

    static boolean readIntEquals(byte[] src, int ref, int sOff) {
        return readInt(src, ref) == readInt(src, sOff);
    }

    static int commonBytes(byte[] src, int ref, int sOff, int srcLimit) {
        int matchLen = 0;
        while (sOff <= srcLimit - 8) {
            if (readLong(src, sOff) == readLong(src, ref)) {
                matchLen += 8;
                ref += 8;
                sOff += 8;
            } else {
                final int zeroBits = Long.numberOfTrailingZeros(readLong(src, sOff) ^ readLong(src, ref));
                return matchLen + (zeroBits >>> 3);
            }
        }
        while (sOff < srcLimit && readByte(src, ref++) == readByte(src, sOff++)) {
            ++matchLen;
        }
        return matchLen;
    }

    static int writeLen(int len, byte[] dest, int dOff) {
        while (len >= 0xFF) {
            writeByte(dest, dOff++, 0xFF);
            len -= 0xFF;
        }
        writeByte(dest, dOff++, len);
        return dOff;
    }

    static int encodeSequence(byte[] src, int anchor, int matchOff, int matchRef, int matchLen, byte[] dest, int dOff, int destEnd) {
        final int runLen = matchOff - anchor;
        final int tokenOff = dOff++;
        int token;

        if (runLen >= RUN_MASK) {
            token = (byte) (RUN_MASK << ML_BITS);
            dOff = writeLen(runLen - RUN_MASK, dest, dOff);
        } else {
            token = runLen << ML_BITS;
        }

        // copy literals
        wildArraycopy(src, anchor, dest, dOff, runLen);
        dOff += runLen;

        // encode offset
        final int matchDec = matchOff - matchRef;
        dest[dOff++] = (byte) matchDec;
        dest[dOff++] = (byte) (matchDec >>> 8);

        // encode match len
        matchLen -= 4;
        if (dOff + (1 + LAST_LITERALS) + (matchLen >>> 8) > destEnd) {
            throw new LZ4Exception("maxDestLen is too small");
        }
        if (matchLen >= ML_MASK) {
            token |= ML_MASK;
            dOff = writeLen(matchLen - RUN_MASK, dest, dOff);
        } else {
            token |= matchLen;
        }

        dest[tokenOff] = (byte) token;

        return dOff;
    }

    static int commonBytesBackward(byte[] b, int o1, int o2, int l1, int l2) {
        int count = 0;
        while (o1 > l1 && o2 > l2 && readByte(b, --o1) == readByte(b, --o2)) {
            ++count;
        }
        return count;
    }

    static int lastLiterals(byte[] src, int sOff, int srcLen, byte[] dest, int dOff, int destEnd) {
        final int runLen = srcLen;

        if (dOff + runLen + 1 + (runLen + 255 - RUN_MASK) / 255 > destEnd) {
            throw new LZ4Exception();
        }

        if (runLen >= RUN_MASK) {
            dest[dOff++] = (byte) (RUN_MASK << ML_BITS);
            dOff = writeLen(runLen - RUN_MASK, dest, dOff);
        } else {
            dest[dOff++] = (byte) (runLen << ML_BITS);
        }
        // copy literals
        System.arraycopy(src, sOff, dest, dOff, runLen);
        dOff += runLen;

        return dOff;
    }
}
