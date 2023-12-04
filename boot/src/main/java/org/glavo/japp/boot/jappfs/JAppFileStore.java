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

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Objects;

public final class JAppFileStore extends FileStore {

    private final JAppFileSystem fs;

    JAppFileStore(JAppFileSystem fs) {
        this.fs = fs;
    }

    @Override
    public String name() {
        return fs + "/";
    }

    @Override
    public String type() {
        return "jappfs";
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        return name.equals("basic");
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return type == BasicFileAttributeView.class;
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        Objects.requireNonNull(type);
        return null;
    }

    @Override
    public long getTotalSpace() throws IOException {
        throw new UnsupportedOperationException("getTotalSpace");
    }

    @Override
    public long getUsableSpace() throws IOException {
        return 0L;
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        return 0L;
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        throw new UnsupportedOperationException("does not support " + attribute);
    }
}
