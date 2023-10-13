package org.glavo.japp;

import java.nio.file.attribute.FileTime;

public final class JAppResource {
    final String name;
    final long offset;
    final long size;
    final FileTime creationTime;
    final FileTime lastModifiedTime;

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
        return creationTime;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public JAppResource(String name, long offset, long size, FileTime creationTime, FileTime lastModifiedTime) {
        this.name = name;
        this.offset = offset;
        this.size = size;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public String toString() {
        return "JAppResource {" +
               "name='" + name + '\'' +
               ", offset=" + offset +
               ", size=" + size +
               ", creationTime=" + creationTime +
               ", lastModifiedTime=" + lastModifiedTime +
               '}';
    }
}
