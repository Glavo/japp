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

import java.util.Arrays;

public final class CompressResult {
    private final CompressionMethod method;
    private final byte[] compressedData;

    private final int offset;
    private final int length;

    public CompressResult(byte[] compressedData) {
        this(CompressionMethod.NONE, compressedData, 0, compressedData.length);
    }

    public CompressResult(CompressionMethod method, byte[] compressedData) {
        this(method, compressedData, 0, compressedData.length);
    }

    public CompressResult(CompressionMethod method, byte[] compressedData, int offset, int length) {
        this.method = method;
        this.compressedData = compressedData;
        this.offset = offset;
        this.length = length;
    }

    public CompressionMethod getMethod() {
        return method;
    }

    public byte[] getCompressedData() {
        return compressedData;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "CompressResult{" +
               "method=" + method +
               ", compressedData=" + Arrays.toString(compressedData) +
               ", offset=" + offset +
               ", length=" + length +
               '}';
    }
}
