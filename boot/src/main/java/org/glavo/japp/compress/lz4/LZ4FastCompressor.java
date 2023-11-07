// Auto-generated: DO NOT EDIT

package org.glavo.japp.compress.lz4;

import static org.glavo.japp.compress.lz4.LZ4Constants.HASH_TABLE_SIZE;
import static org.glavo.japp.compress.lz4.LZ4Constants.HASH_TABLE_SIZE_64K;
import static org.glavo.japp.compress.lz4.LZ4Constants.LAST_LITERALS;
import static org.glavo.japp.compress.lz4.LZ4Constants.LZ4_64K_LIMIT;
import static org.glavo.japp.compress.lz4.LZ4Constants.MAX_DISTANCE;
import static org.glavo.japp.compress.lz4.LZ4Constants.MF_LIMIT;
import static org.glavo.japp.compress.lz4.LZ4Constants.MIN_LENGTH;
import static org.glavo.japp.compress.lz4.LZ4Constants.MIN_MATCH;
import static org.glavo.japp.compress.lz4.LZ4Constants.ML_BITS;
import static org.glavo.japp.compress.lz4.LZ4Constants.ML_MASK;
import static org.glavo.japp.compress.lz4.LZ4Constants.RUN_MASK;
import static org.glavo.japp.compress.lz4.LZ4Constants.SKIP_STRENGTH;
import static org.glavo.japp.compress.lz4.LZ4Utils.hash;
import static org.glavo.japp.compress.lz4.LZ4Utils.hash64k;

import java.util.Arrays;

/**
 * Compressor.
 */
final class LZ4FastCompressor extends LZ4Compressor {

    static final LZ4FastCompressor INSTANCE = new LZ4FastCompressor();

    static int compress64k(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff, int destEnd) {
        final int srcEnd = srcOff + srcLen;
        final int srcLimit = srcEnd - LAST_LITERALS;
        final int mflimit = srcEnd - MF_LIMIT;

        int sOff = srcOff, dOff = destOff;

        int anchor = sOff;

        if (srcLen >= MIN_LENGTH) {

            final short[] hashTable = new short[HASH_TABLE_SIZE_64K];

            ++sOff;

            main:
            while (true) {

                // find a match
                int forwardOff = sOff;

                int ref;
                int step = 1;
                int searchMatchNb = 1 << SKIP_STRENGTH;
                do {
                    sOff = forwardOff;
                    forwardOff += step;
                    step = searchMatchNb++ >>> SKIP_STRENGTH;

                    if (forwardOff > mflimit) {
                        break main;
                    }

                    final int h = hash64k(LZ4Utils.readInt(src, sOff));
                    ref = srcOff + LZ4Utils.readShort(hashTable, h);
                    LZ4Utils.writeShort(hashTable, h, sOff - srcOff);
                } while (!LZ4Utils.readIntEquals(src, ref, sOff));

                // catch up
                final int excess = LZ4Utils.commonBytesBackward(src, ref, sOff, srcOff, anchor);
                sOff -= excess;
                ref -= excess;

                // sequence == refsequence
                final int runLen = sOff - anchor;

                // encode literal length
                int tokenOff = dOff++;

                if (dOff + runLen + (2 + 1 + LAST_LITERALS) + (runLen >>> 8) > destEnd) {
                    throw new LZ4Exception("maxDestLen is too small");
                }

                if (runLen >= RUN_MASK) {
                    LZ4Utils.writeByte(dest, tokenOff, RUN_MASK << ML_BITS);
                    dOff = LZ4Utils.writeLen(runLen - RUN_MASK, dest, dOff);
                } else {
                    LZ4Utils.writeByte(dest, tokenOff, runLen << ML_BITS);
                }

                // copy literals
                LZ4Utils.wildArraycopy(src, anchor, dest, dOff, runLen);
                dOff += runLen;

                while (true) {
                    // encode offset
                    LZ4Utils.writeShort(dest, dOff, (short) (sOff - ref));
                    dOff += 2;

                    // count nb matches
                    sOff += MIN_MATCH;
                    ref += MIN_MATCH;
                    final int matchLen = LZ4Utils.commonBytes(src, ref, sOff, srcLimit);
                    if (dOff + (1 + LAST_LITERALS) + (matchLen >>> 8) > destEnd) {
                        throw new LZ4Exception("maxDestLen is too small");
                    }
                    sOff += matchLen;

                    // encode match len
                    if (matchLen >= ML_MASK) {
                        LZ4Utils.writeByte(dest, tokenOff, LZ4Utils.readByte(dest, tokenOff) | ML_MASK);
                        dOff = LZ4Utils.writeLen(matchLen - ML_MASK, dest, dOff);
                    } else {
                        LZ4Utils.writeByte(dest, tokenOff, LZ4Utils.readByte(dest, tokenOff) | matchLen);
                    }

                    // test end of chunk
                    if (sOff > mflimit) {
                        anchor = sOff;
                        break main;
                    }

                    // fill table
                    LZ4Utils.writeShort(hashTable, hash64k(LZ4Utils.readInt(src, sOff - 2)), sOff - 2 - srcOff);

                    // test next position
                    final int h = hash64k(LZ4Utils.readInt(src, sOff));
                    ref = srcOff + LZ4Utils.readShort(hashTable, h);
                    LZ4Utils.writeShort(hashTable, h, sOff - srcOff);

                    if (!LZ4Utils.readIntEquals(src, sOff, ref)) {
                        break;
                    }

                    tokenOff = dOff++;
                    LZ4Utils.writeByte(dest, tokenOff, 0);
                }

                // prepare next loop
                anchor = sOff++;
            }
        }

        dOff = LZ4Utils.lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
        return dOff - destOff;
    }

    @Override
    public int compress(byte[] src, final int srcOff, int srcLen, byte[] dest, final int destOff, int maxDestLen) {

        LZ4Utils.checkRange(src, srcOff, srcLen);
        LZ4Utils.checkRange(dest, destOff, maxDestLen);
        final int destEnd = destOff + maxDestLen;

        if (srcLen < LZ4_64K_LIMIT) {
            return compress64k(src, srcOff, srcLen, dest, destOff, destEnd);
        }

        final int srcEnd = srcOff + srcLen;
        final int srcLimit = srcEnd - LAST_LITERALS;
        final int mflimit = srcEnd - MF_LIMIT;

        int sOff = srcOff, dOff = destOff;
        int anchor = sOff++;

        final int[] hashTable = new int[HASH_TABLE_SIZE];
        Arrays.fill(hashTable, anchor);

        main:
        while (true) {

            // find a match
            int forwardOff = sOff;

            int ref;
            int step = 1;
            int searchMatchNb = 1 << SKIP_STRENGTH;
            int back;
            do {
                sOff = forwardOff;
                forwardOff += step;
                step = searchMatchNb++ >>> SKIP_STRENGTH;

                if (forwardOff > mflimit) {
                    break main;
                }

                final int h = hash(LZ4Utils.readInt(src, sOff));
                ref = LZ4Utils.readInt(hashTable, h);
                back = sOff - ref;
                LZ4Utils.writeInt(hashTable, h, sOff);
            } while (back >= MAX_DISTANCE || !LZ4Utils.readIntEquals(src, ref, sOff));


            final int excess = LZ4Utils.commonBytesBackward(src, ref, sOff, srcOff, anchor);
            sOff -= excess;
            ref -= excess;

            // sequence == refsequence
            final int runLen = sOff - anchor;

            // encode literal length
            int tokenOff = dOff++;

            if (dOff + runLen + (2 + 1 + LAST_LITERALS) + (runLen >>> 8) > destEnd) {
                throw new LZ4Exception("maxDestLen is too small");
            }

            if (runLen >= RUN_MASK) {
                LZ4Utils.writeByte(dest, tokenOff, RUN_MASK << ML_BITS);
                dOff = LZ4Utils.writeLen(runLen - RUN_MASK, dest, dOff);
            } else {
                LZ4Utils.writeByte(dest, tokenOff, runLen << ML_BITS);
            }

            // copy literals
            LZ4Utils.wildArraycopy(src, anchor, dest, dOff, runLen);
            dOff += runLen;

            while (true) {
                // encode offset
                LZ4Utils.writeShort(dest, dOff, back);
                dOff += 2;

                // count nb matches
                sOff += MIN_MATCH;
                final int matchLen = LZ4Utils.commonBytes(src, ref + MIN_MATCH, sOff, srcLimit);
                if (dOff + (1 + LAST_LITERALS) + (matchLen >>> 8) > destEnd) {
                    throw new LZ4Exception("maxDestLen is too small");
                }
                sOff += matchLen;

                // encode match len
                if (matchLen >= ML_MASK) {
                    LZ4Utils.writeByte(dest, tokenOff, LZ4Utils.readByte(dest, tokenOff) | ML_MASK);
                    dOff = LZ4Utils.writeLen(matchLen - ML_MASK, dest, dOff);
                } else {
                    LZ4Utils.writeByte(dest, tokenOff, LZ4Utils.readByte(dest, tokenOff) | matchLen);
                }

                // test end of chunk
                if (sOff > mflimit) {
                    anchor = sOff;
                    break main;
                }

                // fill table
                LZ4Utils.writeInt(hashTable, hash(LZ4Utils.readInt(src, sOff - 2)), sOff - 2);

                // test next position
                final int h = hash(LZ4Utils.readInt(src, sOff));
                ref = LZ4Utils.readInt(hashTable, h);
                LZ4Utils.writeInt(hashTable, h, sOff);
                back = sOff - ref;

                if (back >= MAX_DISTANCE || !LZ4Utils.readIntEquals(src, ref, sOff)) {
                    break;
                }

                tokenOff = dOff++;
                LZ4Utils.writeByte(dest, tokenOff, 0);
            }

            // prepare next loop
            anchor = sOff++;
        }

        dOff = LZ4Utils.lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
        return dOff - destOff;
    }

}
