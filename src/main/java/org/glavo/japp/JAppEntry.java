package org.glavo.japp;

import java.nio.file.attribute.FileTime;

public final class JAppEntry {
    final String name;
    final long offset;
    final long size;
    final FileTime creationTime;
    final FileTime lastModifiedTime;

    public JAppEntry(String name, long offset, long size, FileTime creationTime, FileTime lastModifiedTime) {
        this.name = name;
        this.offset = offset;
        this.size = size;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public String toString() {
        return "JAppEntry{" +
               "name='" + name + '\'' +
               ", offset=" + offset +
               ", size=" + size +
               ", creationTime=" + creationTime +
               ", lastModifiedTime=" + lastModifiedTime +
               '}';
    }
}
