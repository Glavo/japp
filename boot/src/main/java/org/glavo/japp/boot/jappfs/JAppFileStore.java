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
        throw new UnsupportedOperationException("getUsableSpace");
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        throw new UnsupportedOperationException("getUnallocatedSpace");
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        throw new UnsupportedOperationException("does not support " + attribute);
    }
}
