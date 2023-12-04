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
package org.glavo.japp;

import java.io.IOException;
import java.nio.ByteBuffer;

public enum CompressionMethod {
    NONE,
    CLASSFILE,
    ZSTD;

    private static final CompressionMethod[] METHODS = values();

    public static CompressionMethod of(int i) {
        return i >= 0 && i < METHODS.length ? METHODS[i] : null;
    }

    public static CompressionMethod readFrom(ByteBuffer buffer) throws IOException {
        byte id = buffer.get();
        if (id >= 0 && id < METHODS.length) {
            return METHODS[id];
        }

        throw new IOException(String.format("Unknown compression method: %02x", Byte.toUnsignedInt(id)));
    }

    public byte id() {
        return (byte) ordinal();
    }
}
