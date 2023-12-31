/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.packer.compressor;

import org.glavo.japp.CompressionMethod;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class DefaultCompressor implements Compressor {

    private final Map<String, CompressionMethod> map = new HashMap<>();
    private final CompressionMethod defaultMethod = CompressionMethod.ZSTD;

    public DefaultCompressor() {
        map.put("class", CompressionMethod.CLASSFILE);

        for (String ext : new String[]{
                "png", "apng", "jpg", "jpeg", "webp", "heic", "heif", "avif",
                "aac", "flac", "mp3",
                "mp4", "mkv", "webm",
                "gz", "tgz", "xz", "br", "zst", "bz2", "tbz2"
        }) {
            map.put(ext, CompressionMethod.NONE);
        }
    }

    @Override
    public CompressResult compress(CompressContext context, byte[] source) throws IOException {
        return compress(context, source, null);
    }

    @Override
    public CompressResult compress(CompressContext context, byte[] source, String filePath) throws IOException {
        if (source.length <= 16) {
            return new CompressResult(source);
        }

        String ext;
        if (filePath != null) {
            int sep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            int dot = filePath.lastIndexOf('.');
            ext = dot > sep ? filePath.substring(dot + 1) : null;
        } else {
            ext = null;
        }

        CompressionMethod method = map.getOrDefault(ext, defaultMethod);
        CompressResult result;
        switch (method) {
            case NONE:
                result = new CompressResult(source);
                break;
            case CLASSFILE:
                try {
                    result = Compressors.CLASSFILE.compress(context, source);
                } catch (Throwable e) {
                    // Malformed class file
                    result = Compressors.ZSTD.compress(context, source);
                }
                break;
            case ZSTD:
                result = Compressors.ZSTD.compress(context, source);
                break;
            default:
                throw new AssertionError("Unimplemented compression method: " + method);
        }

        return result.getLength() < source.length ? result : new CompressResult(source);
    }
}
