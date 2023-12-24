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
package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;

public final class JAppResource {

    public static final byte MAGIC_NUMBER = (byte) 0x1b;
    public static final long NO_TIME = Long.MIN_VALUE;

    private final String name;

    private final long offset;
    private final long size;

    private final CompressionMethod method;
    private final long compressedSize;

    private long creationTime = NO_TIME;
    private long lastModifiedTime = NO_TIME;
    private long lastAccessTime = NO_TIME;

    boolean needCheck;
    long checksum;

    public JAppResource(String name, long offset, long size, CompressionMethod method, long compressedSize) {
        this.name = name;
        this.offset = offset;
        this.size = size;
        this.method = method;
        this.compressedSize = compressedSize;
    }

    public String getName() {
        return name;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }

    public FileTime getCreationTime() {
        return creationTime != NO_TIME ? FileTime.fromMillis(creationTime) : FileTime.fromMillis(0L);
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime != NO_TIME ? FileTime.fromMillis(lastModifiedTime) : getCreationTime();
    }

    public FileTime getLastAccessTime() {
        return lastAccessTime != NO_TIME ? FileTime.fromMillis(lastAccessTime) : getLastModifiedTime();
    }

    public CompressionMethod getMethod() {
        return method;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public static JAppResource readFrom(ByteBuffer buffer) throws IOException {
        byte magic = buffer.get();
        if (magic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong resource magic: 0x%02x", magic));
        }

        CompressionMethod compressionMethod = CompressionMethod.readFrom(buffer);

        int pathLength = Short.toUnsignedInt(buffer.getShort());

        int reserved = buffer.getInt();
        if (reserved != 0) {
            throw new IOException("Reserved is not zero");
        }

        long uncompressedSize = buffer.getLong();
        long compressedSize = buffer.getLong();
        long offset = buffer.getLong();

        byte[] pathBuffer = new byte[pathLength];
        buffer.get(pathBuffer);
        String path = new String(pathBuffer, StandardCharsets.UTF_8);

        JAppResource resource = new JAppResource(path, offset, uncompressedSize, compressionMethod, compressedSize);

        int fieldId;
        while ((fieldId = Byte.toUnsignedInt(buffer.get())) != 0) {
            JAppResourceField field = JAppResourceField.of(fieldId);
            if (field == null) {
                throw new IOException(String.format("Unknown field: 0x%02x", fieldId));
            }

            switch (field) {
                case CHECKSUM: {
                    long checksum = buffer.getLong();

                    if (resource.needCheck) {
                        throw new IOException("Duplicate field: " + field);
                    }

                    resource.needCheck = true;
                    resource.checksum = checksum;
                    break;
                }
                case FILE_CREATE_TIME:
                case FILE_LAST_MODIFIED_TIME:
                case FILE_LAST_ACCESS_TIME: {
                    long time = buffer.getLong();
                    if (time == NO_TIME) {
                        throw new IOException("Invalid time: " + time);
                    }

                    long oldTime;
                    if (field == JAppResourceField.FILE_CREATE_TIME) {
                        oldTime = resource.creationTime;
                    } else if (field == JAppResourceField.FILE_LAST_MODIFIED_TIME) {
                        oldTime = resource.lastModifiedTime;
                    } else {
                        oldTime = resource.lastAccessTime;
                    }

                    if (oldTime != NO_TIME) {
                        throw new IOException("Duplicate field: " + field);
                    }

                    if (field == JAppResourceField.FILE_CREATE_TIME) {
                        resource.creationTime = time;
                    } else if (field == JAppResourceField.FILE_LAST_MODIFIED_TIME) {
                        resource.lastModifiedTime = time;
                    } else {
                        resource.lastAccessTime = time;
                    }
                    break;
                }
                default:
                    throw new AssertionError("Field: " + field);
            }
        }

        return resource;
    }

    @Override
    public String toString() {
        return String.format(
                "JAppResource[name=%s, offset=%d, size=%d, method=%s, compressedSize=%d, creationTime=%d, lastModifiedTime=%d, lastAccessTime=%d, needCheck=%s, checksum=%d]",
                name, offset, size, method, compressedSize, creationTime, lastModifiedTime, lastAccessTime, needCheck, checksum);
    }
}
