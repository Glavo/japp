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
package org.glavo.japp.boot.decompressor.zstd;

import org.glavo.japp.util.UnsafeUtil;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.glavo.japp.util.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;

public final class ZstdUtils {
    public static int maxCompressedLength(int sourceLength) {
        int maxCompressedSize = sourceLength + (sourceLength >>> 8);

        if (sourceLength < 128 * 1024) {
            maxCompressedSize += (128 * 1024 - sourceLength) >>> 11;
        }
        return maxCompressedSize;
    }

    private static final ThreadLocal<WeakReference<ZstdFrameDecompressor>> threadLocalDecompressor = new ThreadLocal<>();

    private static ZstdFrameDecompressor decompressor() {
        WeakReference<ZstdFrameDecompressor> decompressorReference = threadLocalDecompressor.get();
        ZstdFrameDecompressor decompressor;
        if (decompressorReference != null && (decompressor = decompressorReference.get()) != null) {
            return decompressor;
        }

        decompressor = new ZstdFrameDecompressor();
        threadLocalDecompressor.set(new WeakReference<>(decompressor));
        return decompressor;
    }

    public static int decompress(ByteBuffer input, ByteBuffer output) throws MalformedInputException {
        Object inputBase;
        long inputBaseAddress;
        long inputAddress;
        long inputLimit;

        Object outputBase;
        long outputBaseAddress;
        long outputAddress;
        long outputLimit;

        if (input.hasArray()) {
            inputBase = input.array();
            inputBaseAddress = ARRAY_BYTE_BASE_OFFSET + input.arrayOffset();
        } else {
            inputBase = null;
            inputBaseAddress = UnsafeUtil.getDirectBufferAddress(input);
        }
        inputAddress = inputBaseAddress + input.position();
        inputLimit = inputBaseAddress + input.limit();

        if (output.hasArray()) {
            outputBase = output.array();
            outputBaseAddress = ARRAY_BYTE_BASE_OFFSET + output.arrayOffset();
        } else {
            outputBase = null;
            outputBaseAddress = UnsafeUtil.getDirectBufferAddress(output);
        }
        outputAddress = outputBaseAddress + output.position();
        outputLimit = outputBaseAddress + output.limit();

        input.position(input.limit());
        int n = decompressor().decompress(inputBase, inputAddress, inputLimit, outputBase, outputAddress, outputLimit);
        output.position(output.position() + n);
        return n;
    }

    public static int decompress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset, int maxOutputLength)
            throws MalformedInputException {
        verifyRange(input, inputOffset, inputLength);
        verifyRange(output, outputOffset, maxOutputLength);

        long inputAddress = ARRAY_BYTE_BASE_OFFSET + inputOffset;
        long inputLimit = inputAddress + inputLength;
        long outputAddress = ARRAY_BYTE_BASE_OFFSET + outputOffset;
        long outputLimit = outputAddress + maxOutputLength;

        try {
            return decompressor().decompress(input, inputAddress, inputLimit, output, outputAddress, outputLimit);
        } finally {
            Reference.reachabilityFence(input);
            Reference.reachabilityFence(output);
        }
    }

    private static void verifyRange(byte[] data, int offset, int length) {
        requireNonNull(data, "data is null");
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IllegalArgumentException(format("Invalid offset or length (%s, %s) in array of length %s", offset, length, data.length));
        }
    }

    private ZstdUtils() {
    }
}
