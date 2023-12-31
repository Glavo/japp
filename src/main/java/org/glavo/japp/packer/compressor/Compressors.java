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
import org.glavo.japp.util.ZstdUtils;
import org.glavo.japp.packer.compressor.classfile.ClassFileCompressor;

public final class Compressors {

    public static final Compressor DEFAULT = new DefaultCompressor();

    public static final Compressor CLASSFILE = new ClassFileCompressor();

    public static final Compressor ZSTD = (context, source) -> {
        byte[] res = new byte[ZstdUtils.maxCompressedLength(source.length)];
        long n = context.getZstdCompressCtx().compressByteArray(res, 0, res.length, source, 0, source.length);
        return new CompressResult(CompressionMethod.ZSTD, res, 0, (int) n);
    };

    private Compressors() {
    }
}
