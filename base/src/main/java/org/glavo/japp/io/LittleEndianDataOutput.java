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
package org.glavo.japp.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

public interface LittleEndianDataOutput extends Closeable {

    static LittleEndianDataOutput of(OutputStream outputStream) {
        return new WritableByteChannelWrapper(Channels.newChannel(outputStream));
    }

    static LittleEndianDataOutput of(WritableByteChannel channel) {
        return new WritableByteChannelWrapper(channel);
    }

    long getTotalBytes();

    void writeByte(byte v) throws IOException;

    default void writeUnsignedByte(int v) throws IOException {
        if (v < 0 || v > 0xff) {
            throw new IllegalArgumentException();
        }

        writeByte((byte) v);
    }

    void writeShort(short v) throws IOException;

    default void writeUnsignedShort(int v) throws IOException {
        if (v < 0 || v > 0xffff) {
            throw new IllegalArgumentException();
        }

        writeShort((short) v);
    }

    void writeInt(int v) throws IOException;

    default void writeUnsignedInt(long v) throws IOException {
        if (v < 0 || v > 0xffff_ffffL) {
            throw new IllegalArgumentException();
        }

        writeInt((int) v);
    }

    void writeLong(long v) throws IOException;

    default void writeBytes(byte[] array) throws IOException {
        writeBytes(array, 0, array.length);
    }

    void writeBytes(byte[] array, int offset, int len) throws IOException;

    default void writeString(String str) throws IOException {
        if (str != null) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            writeInt(bytes.length);
            writeBytes(bytes);
        } else {
            writeInt(0);
        }
    }
}
