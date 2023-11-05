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

import static org.glavo.japp.compress.lz4.LZ4Constants.*;
import static org.glavo.japp.compress.lz4.LZ4Constants.COPY_LENGTH;

/**
 * LZ4 decompressor that requires the size of the original input to be known.
 * Use {@code LZ4SafeDecompressor} if you only know the size of the
 * compressed stream.
 * <p>
 * Instances of this class are thread-safe.
 */
public final class LZ4Decompressor {

    private LZ4Decompressor() {
    }

    /**
     * Decompresses <code>src[srcOff:]</code> into <code>dest[destOff:destOff+destLen]</code>
     * and returns the number of bytes read from <code>src</code>.
     * <code>destLen</code> must be exactly the size of the decompressed data.
     *
     * @param src     the compressed data
     * @param srcOff  the start offset in src
     * @param dest    the destination buffer to store the decompressed data
     * @param destOff the start offset in dest
     * @param destLen the <b>exact</b> size of the original input
     * @return the number of bytes read to restore the original input
     */
    public static int decompress(byte[] src, final int srcOff, byte[] dest, final int destOff, int destLen) {
        LZ4Utils.checkRange(src, srcOff);
        LZ4Utils.checkRange(dest, destOff, destLen);

        if (destLen == 0) {
            if (LZ4Utils.readByte(src, srcOff) != 0) {
                throw new LZ4Exception("Malformed input at " + srcOff);
            }
            return 1;
        }


        final int destEnd = destOff + destLen;

        int sOff = srcOff;
        int dOff = destOff;

        while (true) {
            final int token = LZ4Utils.readByte(src, sOff) & 0xFF;
            ++sOff;

            // literals
            int literalLen = token >>> ML_BITS;
            if (literalLen == RUN_MASK) {
                byte len = (byte) 0xFF;
                while ((len = LZ4Utils.readByte(src, sOff++)) == (byte) 0xFF) {
                    literalLen += 0xFF;
                }
                literalLen += len & 0xFF;
            }

            final int literalCopyEnd = dOff + literalLen;

            if (literalCopyEnd > destEnd - COPY_LENGTH) {
                if (literalCopyEnd != destEnd) {
                    throw new LZ4Exception("Malformed input at " + sOff);

                } else {
                    LZ4Utils.safeArraycopy(src, sOff, dest, dOff, literalLen);
                    sOff += literalLen;
                    dOff = literalCopyEnd;
                    break; // EOF
                }
            }

            LZ4Utils.wildArraycopy(src, sOff, dest, dOff, literalLen);
            sOff += literalLen;
            dOff = literalCopyEnd;

            // matchs
            final int matchDec = Short.toUnsignedInt(LZ4Utils.readShort(src, sOff));
            sOff += 2;
            int matchOff = dOff - matchDec;

            if (matchOff < destOff) {
                throw new LZ4Exception("Malformed input at " + sOff);
            }

            int matchLen = token & ML_MASK;
            if (matchLen == ML_MASK) {
                byte len = (byte) 0xFF;
                while ((len = LZ4Utils.readByte(src, sOff++)) == (byte) 0xFF) {
                    matchLen += 0xFF;
                }
                matchLen += len & 0xFF;
            }
            matchLen += MIN_MATCH;

            final int matchCopyEnd = dOff + matchLen;

            if (matchCopyEnd > destEnd - COPY_LENGTH) {
                if (matchCopyEnd > destEnd) {
                    throw new LZ4Exception("Malformed input at " + sOff);
                }
                LZ4Utils.safeIncrementalCopy(dest, matchOff, dOff, matchLen);
            } else {
                LZ4Utils.wildIncrementalCopy(dest, matchOff, dOff, matchCopyEnd);
            }
            dOff = matchCopyEnd;
        }


        return sOff - srcOff;

    }

    /**
     * Convenience method, equivalent to calling
     * {@link #decompress(byte[], int, byte[], int, int) decompress(src, 0, dest, 0, destLen)}.
     *
     * @param src     the compressed data
     * @param dest    the destination buffer to store the decompressed data
     * @param destLen the <b>exact</b> size of the original input
     * @return the number of bytes read to restore the original input
     */
    public static int decompress(byte[] src, byte[] dest, int destLen) {
        return decompress(src, 0, dest, 0, destLen);
    }

    /**
     * Convenience method, equivalent to calling
     * {@link #decompress(byte[], byte[], int) decompress(src, dest, dest.length)}.
     *
     * @param src  the compressed data
     * @param dest the destination buffer to store the decompressed data
     * @return the number of bytes read to restore the original input
     */
    public static int decompress(byte[] src, byte[] dest) {
        return decompress(src, dest, dest.length);
    }

    /**
     * Convenience method which returns <code>src[srcOff:?]</code>
     * decompressed.
     * <p><b><span style="color:red">Warning</span></b>: this method has an
     * important overhead due to the fact that it needs to allocate a buffer to
     * decompress into.</p>
     * <p>Here is how this method is implemented:</p>
     * <pre>
     * final byte[] decompressed = new byte[destLen];
     * decompress(src, srcOff, decompressed, 0, destLen);
     * return decompressed;
     * </pre>
     *
     * @param src     the compressed data
     * @param srcOff  the start offset in src
     * @param destLen the <b>exact</b> size of the original input
     * @return the decompressed data
     */
    public static byte[] decompress(byte[] src, int srcOff, int destLen) {
        final byte[] decompressed = new byte[destLen];
        decompress(src, srcOff, decompressed, 0, destLen);
        return decompressed;
    }

    /**
     * Convenience method, equivalent to calling
     * {@link #decompress(byte[], int, int) decompress(src, 0, destLen)}.
     *
     * @param src     the compressed data
     * @param destLen the <b>exact</b> size of the original input
     * @return the decompressed data
     */
    public static byte[] decompress(byte[] src, int destLen) {
        return decompress(src, 0, destLen);
    }

}
