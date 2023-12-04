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

import org.glavo.japp.TODO;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class JAppFileAttributes implements BasicFileAttributes {

    private final JAppFileSystem.Node node;

    public JAppFileAttributes(JAppFileSystem.Node node) {
        this.node = node;
    }

    @Override
    public FileTime lastModifiedTime() {
        throw new TODO();
    }

    @Override
    public FileTime lastAccessTime() {
        throw new TODO();
    }

    @Override
    public FileTime creationTime() {
        throw new TODO();
    }

    @Override
    public boolean isRegularFile() {
        throw new TODO();
    }

    @Override
    public boolean isDirectory() {
        throw new TODO();
    }

    @Override
    public boolean isSymbolicLink() {
        throw new TODO();
    }

    @Override
    public boolean isOther() {
        throw new TODO();
    }

    @Override
    public long size() {
        throw new TODO();
    }

    @Override
    public Object fileKey() {
        throw new TODO();
    }
}
