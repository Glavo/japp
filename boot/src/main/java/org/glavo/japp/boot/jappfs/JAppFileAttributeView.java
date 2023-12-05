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

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

public final class JAppFileAttributeView implements BasicFileAttributeView {

    private final JAppPath path;
    private final boolean isJAppView;
    private final LinkOption[] options;

    public JAppFileAttributeView(JAppPath path, boolean isJAppView, LinkOption... options) {
        this.path = path;
        this.isJAppView = isJAppView;
        this.options = options;
    }

    @Override
    public String name() {
        return isJAppView ? "japp" : "basic";
    }

    @Override
    public JAppFileAttributes readAttributes() throws IOException {
        throw new TODO();
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        throw new ReadOnlyFileSystemException();
    }
}
