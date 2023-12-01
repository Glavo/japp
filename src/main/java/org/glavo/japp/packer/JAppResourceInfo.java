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
