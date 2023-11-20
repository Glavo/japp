package org.glavo.japp.boot.jappfs;

import org.glavo.japp.TODO;

import java.io.IOException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

public final class JAppFileAttributeView implements BasicFileAttributeView {

    private final boolean isJAppView;

    public JAppFileAttributeView(boolean isJAppView) {
        this.isJAppView = isJAppView;
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
