// Auto-generated: DO NOT EDIT

package org.glavo.japp.compress.lz4;

import static org.glavo.japp.compress.lz4.LZ4Constants.DEFAULT_COMPRESSION_LEVEL;
import static org.glavo.japp.compress.lz4.LZ4Constants.HASH_TABLE_SIZE_HC;
import static org.glavo.japp.compress.lz4.LZ4Constants.LAST_LITERALS;
import static org.glavo.japp.compress.lz4.LZ4Constants.MAX_DISTANCE;
import static org.glavo.japp.compress.lz4.LZ4Constants.MF_LIMIT;
import static org.glavo.japp.compress.lz4.LZ4Constants.MIN_MATCH;
import static org.glavo.japp.compress.lz4.LZ4Constants.ML_MASK;
import static org.glavo.japp.compress.lz4.LZ4Constants.OPTIMAL_ML;
import static org.glavo.japp.compress.lz4.LZ4Utils.hashHC;

import java.util.Arrays;

/**
 * High compression compressor.
 */
final class LZ4HCCompressor extends LZ4Compressor {

    static final LZ4HCCompressor INSTANCE = new LZ4HCCompressor();

    private static final int maxAttempts = 1 << (DEFAULT_COMPRESSION_LEVEL - 1);

    private static final class HashTable {
        static final int MASK = MAX_DISTANCE - 1;
        int nextToUpdate;
        private final int base;
        private final int[] hashTable;
        private final short[] chainTable;

        HashTable(int base) {
            this.base = base;
            nextToUpdate = base;
            hashTable = new int[HASH_TABLE_SIZE_HC];
            Arrays.fill(hashTable, -1);
            chainTable = new short[MAX_DISTANCE];
        }

        private int hashPointer(byte[] bytes, int off) {
            final int v = LZ4Utils.readInt(bytes, off);
            return hashPointer(v);
        }

        private int hashPointer(int v) {
            final int h = hashHC(v);
            return hashTable[h];
        }

        private int next(int off) {
            return off - (chainTable[off & MASK] & 0xFFFF);
        }

        private void addHash(byte[] bytes, int off) {
            final int v = LZ4Utils.readInt(bytes, off);
            addHash(v, off);
        }

        private void addHash(int v, int off) {
            final int h = hashHC(v);
            int delta = off - hashTable[h];
            assert delta > 0 : delta;
            if (delta >= MAX_DISTANCE) {
                delta = MAX_DISTANCE - 1;
            }
            chainTable[off & MASK] = (short) delta;
            hashTable[h] = off;
        }

        void insert(int off, byte[] bytes) {
            for (; nextToUpdate < off; ++nextToUpdate) {
                addHash(bytes, nextToUpdate);
            }
        }

        boolean insertAndFindBestMatch(byte[] buf, int off, int matchLimit, Match match) {
            match.start = off;
            match.len = 0;
            int delta = 0;
            int repl = 0;

            insert(off, buf);

            int ref = hashPointer(buf, off);

            if (ref >= off - 4 && ref <= off && ref >= base) { // potential repetition
                if (LZ4Utils.readIntEquals(buf, ref, off)) { // confirmed
                    delta = off - ref;
                    repl = match.len = MIN_MATCH + LZ4Utils.commonBytes(buf, ref + MIN_MATCH, off + MIN_MATCH, matchLimit);
                    match.ref = ref;
                }
                ref = next(ref);
            }

            for (int i = 0; i < maxAttempts; ++i) {
                if (ref < Math.max(base, off - MAX_DISTANCE + 1) || ref > off) {
                    break;
                }
                if (LZ4Utils.readIntEquals(buf, ref, off)) {
                    final int matchLen = MIN_MATCH + LZ4Utils.commonBytes(buf, ref + MIN_MATCH, off + MIN_MATCH, matchLimit);
                    if (matchLen > match.len) {
                        match.ref = ref;
                        match.len = matchLen;
                    }
                }
                ref = next(ref);
            }

            if (repl != 0) {
                int ptr = off;
                final int end = off + repl - (MIN_MATCH - 1);
                while (ptr < end - delta) {
                    chainTable[ptr & MASK] = (short) delta; // pre load
                    ++ptr;
                }
                do {
                    chainTable[ptr & MASK] = (short) delta;
                    hashTable[hashHC(LZ4Utils.readInt(buf, ptr))] = ptr;
                    ++ptr;
                } while (ptr < end);
                nextToUpdate = end;
            }

            return match.len != 0;
        }

        boolean insertAndFindWiderMatch(byte[] buf, int off, int startLimit, int matchLimit, int minLen, Match match) {
            match.len = minLen;

            insert(off, buf);

            final int delta = off - startLimit;
            int ref = hashPointer(buf, off);
            for (int i = 0; i < maxAttempts; ++i) {
                if (ref < Math.max(base, off - MAX_DISTANCE + 1) || ref > off) {
                    break;
                }
                if (LZ4Utils.readIntEquals(buf, ref, off)) {
                    final int matchLenForward = MIN_MATCH + LZ4Utils.commonBytes(buf, ref + MIN_MATCH, off + MIN_MATCH, matchLimit);
                    final int matchLenBackward = LZ4Utils.commonBytesBackward(buf, ref, off, base, startLimit);
                    final int matchLen = matchLenBackward + matchLenForward;
                    if (matchLen > match.len) {
                        match.len = matchLen;
                        match.ref = ref - matchLenBackward;
                        match.start = off - matchLenBackward;
                    }
                }
                ref = next(ref);
            }

            return match.len > minLen;
        }

    }

    @Override
    public int compress(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff, int maxDestLen) {
        LZ4Utils.checkRange(src, srcOff, srcLen);
        LZ4Utils.checkRange(dest, destOff, maxDestLen);

        final int srcEnd = srcOff + srcLen;
        final int destEnd = destOff + maxDestLen;
        final int mfLimit = srcEnd - MF_LIMIT;
        final int matchLimit = srcEnd - LAST_LITERALS;

        int sOff = srcOff;
        int dOff = destOff;
        int anchor = sOff++;

        final HashTable ht = new HashTable(srcOff);
        final Match match0 = new Match();
        final Match match1 = new Match();
        final Match match2 = new Match();
        final Match match3 = new Match();

        main:
        while (sOff < mfLimit) {
            if (!ht.insertAndFindBestMatch(src, sOff, matchLimit, match1)) {
                ++sOff;
                continue;
            }

            // saved, in case we would skip too much
            match1.copyTo(match0);

            search2:
            while (true) {
                assert match1.start >= anchor;
                if (match1.end() >= mfLimit
                    || !ht.insertAndFindWiderMatch(src, match1.end() - 2, match1.start + 1, matchLimit, match1.len, match2)) {
                    // no better match
                    dOff = LZ4Utils.encodeSequence(src, anchor, match1.start, match1.ref, match1.len, dest, dOff, destEnd);
                    anchor = sOff = match1.end();
                    continue main;
                }

                if (match0.start < match1.start) {
                    if (match2.start < match1.start + match0.len) { // empirical
                        match0.copyTo(match1);
                    }
                }
                assert match2.start > match1.start;

                if (match2.start - match1.start < 3) { // First Match too small : removed
                    match2.copyTo(match1);
                    continue search2;
                }

                search3:
                while (true) {
                    if (match2.start - match1.start < OPTIMAL_ML) {
                        int newMatchLen = match1.len;
                        if (newMatchLen > OPTIMAL_ML) {
                            newMatchLen = OPTIMAL_ML;
                        }
                        if (match1.start + newMatchLen > match2.end() - MIN_MATCH) {
                            newMatchLen = match2.start - match1.start + match2.len - MIN_MATCH;
                        }
                        final int correction = newMatchLen - (match2.start - match1.start);
                        if (correction > 0) {
                            match2.fix(correction);
                        }
                    }

                    if (match2.start + match2.len >= mfLimit
                        || !ht.insertAndFindWiderMatch(src, match2.end() - 3, match2.start, matchLimit, match2.len, match3)) {
                        // no better match -> 2 sequences to encode
                        if (match2.start < match1.end()) {
                            match1.len = match2.start - match1.start;
                        }
                        // encode seq 1
                        dOff = LZ4Utils.encodeSequence(src, anchor, match1.start, match1.ref, match1.len, dest, dOff, destEnd);
                        anchor = sOff = match1.end();
                        // encode seq 2
                        dOff = LZ4Utils.encodeSequence(src, anchor, match2.start, match2.ref, match2.len, dest, dOff, destEnd);
                        anchor = sOff = match2.end();
                        continue main;
                    }

                    if (match3.start < match1.end() + 3) { // Not enough space for match 2 : remove it
                        if (match3.start >= match1.end()) { // // can write Seq1 immediately ==> Seq2 is removed, so Seq3 becomes Seq1
                            if (match2.start < match1.end()) {
                                final int correction = match1.end() - match2.start;
                                match2.fix(correction);
                                if (match2.len < MIN_MATCH) {
                                    match3.copyTo(match2);
                                }
                            }

                            dOff = LZ4Utils.encodeSequence(src, anchor, match1.start, match1.ref, match1.len, dest, dOff, destEnd);
                            anchor = sOff = match1.end();

                            match3.copyTo(match1);
                            match2.copyTo(match0);

                            continue search2;
                        }

                        match3.copyTo(match2);
                        continue search3;
                    }

                    // OK, now we have 3 ascending matches; let's write at least the first one
                    if (match2.start < match1.end()) {
                        if (match2.start - match1.start < ML_MASK) {
                            if (match1.len > OPTIMAL_ML) {
                                match1.len = OPTIMAL_ML;
                            }
                            if (match1.end() > match2.end() - MIN_MATCH) {
                                match1.len = match2.end() - match1.start - MIN_MATCH;
                            }
                            final int correction = match1.end() - match2.start;
                            match2.fix(correction);
                        } else {
                            match1.len = match2.start - match1.start;
                        }
                    }

                    dOff = LZ4Utils.encodeSequence(src, anchor, match1.start, match1.ref, match1.len, dest, dOff, destEnd);
                    anchor = sOff = match1.end();

                    match2.copyTo(match1);
                    match3.copyTo(match2);

                    continue search3;
                }

            }

        }

        dOff = LZ4Utils.lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
        return dOff - destOff;
    }

}
