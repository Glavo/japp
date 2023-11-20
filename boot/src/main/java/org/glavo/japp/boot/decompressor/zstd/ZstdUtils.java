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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.glavo.japp.boot.decompressor.zstd.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;

public final class ZstdUtils {
    private static final ZstdFrameDecompressor decompressor = new ZstdFrameDecompressor();

    public static int decompress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset, int maxOutputLength)
            throws MalformedInputException {
        verifyRange(input, inputOffset, inputLength);
        verifyRange(output, outputOffset, maxOutputLength);

        long inputAddress = ARRAY_BYTE_BASE_OFFSET + inputOffset;
        long inputLimit = inputAddress + inputLength;
        long outputAddress = ARRAY_BYTE_BASE_OFFSET + outputOffset;
        long outputLimit = outputAddress + maxOutputLength;

        return decompressor.decompress(input, inputAddress, inputLimit, output, outputAddress, outputLimit);
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
