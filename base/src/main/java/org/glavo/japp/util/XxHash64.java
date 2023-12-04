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

import java.lang.ref.Reference;
import java.nio.ByteBuffer;

import static org.glavo.japp.util.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;

public final class XxHash64 {
    private static final long P1 = 0x9E3779B185EBCA87L;
    private static final long P2 = 0xC2B2AE3D27D4EB4FL;
    private static final long P3 = 0x165667B19E3779F9L;
    private static final long P4 = 0x85EBCA77C2b2AE63L;
    private static final long P5 = 0x27D4EB2F165667C5L;

    public static long hashByteBufferWithoutUpdate(ByteBuffer buffer) {
        return hashByteBufferWithoutUpdate(0L, buffer);
    }

    public static long hashByteBufferWithoutUpdate(long seed, ByteBuffer buffer) {
        Object inputBase;
        long inputAddress;
        long inputLimit;

        if (buffer.hasArray()) {
            inputBase = buffer.array();
            inputAddress = ARRAY_BYTE_BASE_OFFSET + buffer.arrayOffset() + buffer.position();
        } else {
            inputBase = null;
            inputAddress = UnsafeUtil.getDirectBufferAddress(buffer);
        }
        inputLimit = inputAddress + buffer.remaining();

        try {
            return hash(seed, inputBase, inputAddress, inputLimit);
        } finally {
            Reference.reachabilityFence(buffer);
        }
    }

    public static long hash(byte[] array) {
        return hash(0, array);
    }

    public static long hash(long seed, byte[] array) {
        return hash(seed, array, 0, array.length);
    }

    public static long hash(long seed, byte[] array, int offset, int length) {
        return hash(seed, (Object) array, ARRAY_BYTE_BASE_OFFSET + offset, ARRAY_BYTE_BASE_OFFSET + offset + length);
    }

    public static long hash(long seed, Object inputBase, long inputAddress, long inputLimit) {
        long hash;
        long address = inputAddress;

        if (inputLimit - address >= 32) {
            long v1 = seed + P1 + P2;
            long v2 = seed + P2;
            long v3 = seed;
            long v4 = seed - P1;

            do {
                v1 = mix(v1, UnsafeUtil.getLong(inputBase, address));
                v2 = mix(v2, UnsafeUtil.getLong(inputBase, address + 8));
                v3 = mix(v3, UnsafeUtil.getLong(inputBase, address + 16));
                v4 = mix(v4, UnsafeUtil.getLong(inputBase, address + 24));

                address += 32;
            } while (inputLimit - address >= 32);

            hash = Long.rotateLeft(v1, 1)
                   + Long.rotateLeft(v2, 7)
                   + Long.rotateLeft(v3, 12)
                   + Long.rotateLeft(v4, 18);

            hash = update(hash, v1);
            hash = update(hash, v2);
            hash = update(hash, v3);
            hash = update(hash, v4);
        } else {
            hash = seed + P5;
        }

        hash += inputLimit - inputAddress;

        while (address <= inputLimit - 8) {
            long k1 = UnsafeUtil.getLong(inputBase, address);
            k1 *= P2;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= P1;
            hash ^= k1;
            hash = Long.rotateLeft(hash, 27) * P1 + P4;
            address += 8;
        }

        if (address <= inputLimit - 4) {
            hash ^= UnsafeUtil.getUnsignedInt(inputBase, address) * P1;
            hash = Long.rotateLeft(hash, 23) * P2 + P3;
            address += 4;
        }

        while (address < inputLimit) {
            hash ^= UnsafeUtil.getUnsignedByte(inputBase, address) * P5;
            hash = Long.rotateLeft(hash, 11) * P1;
            address++;
        }

        return finalize(hash);
    }

    private static long mix(long current, long value) {
        return Long.rotateLeft(current + value * P2, 31) * P1;
    }

    private static long update(long hash, long value) {
        return (hash ^ mix(0, value)) * P1 + P4;
    }

    private static long finalize(long hash) {
        hash ^= hash >>> 33;
        hash *= P2;
        hash ^= hash >>> 29;
        hash *= P3;
        hash ^= hash >>> 32;
        return hash;
    }

}

