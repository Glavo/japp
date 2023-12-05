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
package org.glavo.japp.boot.jappfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class JAppFileAttributes implements BasicFileAttributes {

    enum Attribute {
        // basic
        size,
        creationTime,
        lastAccessTime,
        lastModifiedTime,
        isDirectory,
        isRegularFile,
        isSymbolicLink,
        isOther,
        fileKey,

        // japp
        compressedSize
    }

    private final JAppFileSystem fileSystem;
    private final JAppFileSystem.Node node;

    public JAppFileAttributes(JAppFileSystem fileSystem, JAppFileSystem.Node node) {
        this.fileSystem = fileSystem;
        this.node = node;
    }

    private FileTime getDefaultFileTime() {
        return FileTime.fromMillis(0);
    }

    Object getAttribute(Attribute attribute) {
        switch (attribute) {
            case size:
                return size();
            case creationTime:
                return creationTime();
            case lastAccessTime:
                return lastAccessTime();
            case lastModifiedTime:
                return lastModifiedTime();
            case isDirectory:
                return isDirectory();
            case isRegularFile:
                return isRegularFile();
            case isSymbolicLink:
                return isSymbolicLink();
            case isOther:
                return isOther();
            case fileKey:
                return fileKey();
            case compressedSize:
                return compressedSize();
            default:
                return null;
        }
    }

    @Override
    public FileTime lastModifiedTime() {
        if (node instanceof JAppFileSystem.ResourceNode) {
            return ((JAppFileSystem.ResourceNode) node).getResource().getLastModifiedTime();
        } else {
            return getDefaultFileTime();
        }
    }

    @Override
    public FileTime lastAccessTime() {
        if (node instanceof JAppFileSystem.ResourceNode) {
            return ((JAppFileSystem.ResourceNode) node).getResource().getLastAccessTime();
        } else {
            return getDefaultFileTime();
        }
    }

    @Override
    public FileTime creationTime() {
        if (node instanceof JAppFileSystem.ResourceNode) {
            return ((JAppFileSystem.ResourceNode) node).getResource().getCreationTime();
        } else {
            return getDefaultFileTime();
        }
    }

    @Override
    public boolean isRegularFile() {
        return node instanceof JAppFileSystem.ResourceNode;
    }

    @Override
    public boolean isDirectory() {
        return node instanceof JAppFileSystem.DirectoryNode;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return (node instanceof JAppFileSystem.ResourceNode) ? ((JAppFileSystem.ResourceNode) node).getResource().getSize() : 0L;
    }

    @Override
    public Object fileKey() {
        return node;
    }

    // JApp

    public long compressedSize() {
        return (node instanceof JAppFileSystem.ResourceNode) ? ((JAppFileSystem.ResourceNode) node).getResource().getCompressedSize() : 0L;
    }
}
