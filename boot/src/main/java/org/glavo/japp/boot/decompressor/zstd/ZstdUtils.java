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

import java.lang.ref.WeakReference;

public final class ZstdUtils {
    public static int maxCompressedLength(int sourceLength) {
        int maxCompressedSize = sourceLength + (sourceLength >>> 8);

        if (sourceLength < 128 * 1024) {
            maxCompressedSize += (128 * 1024 - sourceLength) >>> 11;
        }
        return maxCompressedSize;
    }

    private static final ThreadLocal<WeakReference<ZstdFrameDecompressor>> threadLocalDecompressor = new ThreadLocal<>();

    public static ZstdFrameDecompressor decompressor() {
        WeakReference<ZstdFrameDecompressor> decompressorReference = threadLocalDecompressor.get();
        ZstdFrameDecompressor decompressor;
        if (decompressorReference != null && (decompressor = decompressorReference.get()) != null) {
            return decompressor;
        }

        decompressor = new ZstdFrameDecompressor();
        threadLocalDecompressor.set(new WeakReference<>(decompressor));
        return decompressor;
    }

    private ZstdUtils() {
    }
}
