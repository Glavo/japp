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
package org.glavo.japp.packer;

import org.glavo.japp.CompressionMethod;

import java.nio.file.attribute.FileTime;

public final class JAppResourceInfo {
    final String name;

    FileTime creationTime;
    FileTime lastModifiedTime;
    FileTime lastAccessTime;

    boolean hasWritten = false;
    long offset;
    long size;
    CompressionMethod method;
    long compressedSize;

    Long checksum;

    public JAppResourceInfo(String name) {
        this.name = name;
    }

    public void setCreationTime(FileTime creationTime) {
        this.creationTime = creationTime;
    }

    public void setLastModifiedTime(FileTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public void setLastAccessTime(FileTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
}
