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
package org.glavo.japp.boot;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JAppBootArgs {
    public enum Field {
        END,
        MAIN_CLASS,
        MAIN_MODULE,
        MODULE_PATH,
        CLASS_PATH,
        ADD_READS,
        ADD_EXPORTS,
        ADD_OPENS,
        ENABLE_NATIVE_ACCESS;

        private static final Field[] VALUES = values();

        public static Field readFrom(ByteBuffer buffer) throws IOException {
            byte id = buffer.get();
            if (id >= 0 && id < VALUES.length) {
                return VALUES[id];
            }
            throw new IOException(String.format("Unknown field: 0x%02x", Byte.toUnsignedInt(id)));
        }

        public byte id() {
            return (byte) ordinal();
        }
    }

    public static final byte ID_RESOLVED_REFERENCE_END = 0;
    public static final byte ID_RESOLVED_REFERENCE_LOCAL = 1;
    public static final byte ID_RESOLVED_REFERENCE_EXTERNAL = 2;

    String mainClass;
    String mainModule;

    final List<String> addReads = new ArrayList<>();
    final List<String> addExports = new ArrayList<>();
    final List<String> addOpens = new ArrayList<>();

    final List<String> enableNativeAccess = new ArrayList<>();

    final List<Path> externalModules = new ArrayList<>();
}
