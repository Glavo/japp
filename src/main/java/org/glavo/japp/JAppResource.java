package org.glavo.japp;

import java.nio.file.attribute.FileTime;

public final class JAppResource {
    public enum Type {
        GROUP,
        FILE;
    }
    public static final String MODULES = "/modules/";
    public static final String CLASSPATH = "/classpath/";

    private final String name;
    private final Type type;

    private final long offset;
    private final long size;
    private final FileTime lastAccessTime;

    private final FileTime lastModifiedTime;
    private final FileTime creationTime;

    private final CompressionMethod method;
    private final long compressedSize;

    public JAppResource(String name, Type type, long offset, long size, FileTime lastAccessTime, FileTime lastModifiedTime, FileTime creationTime, CompressionMethod method, long compressedSize) {
        this.name = name;
        this.type = type;
        this.offset = offset;
        this.size = size;
        this.lastAccessTime = lastAccessTime;
        this.lastModifiedTime = lastModifiedTime;
        this.creationTime = creationTime;
        this.method = method;
        this.compressedSize = compressedSize;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }

    public FileTime getLastAccessTime() {
        return lastAccessTime;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public FileTime getCreationTime() {
        return creationTime;
    }

    public CompressionMethod getMethod() {
        return method;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    @Override
    public String toString() {
        return "JAppResource{" +
               "name=" + name +
               ", type=" + type +
               ", offset=" + offset +
               ", size=" + size +
               ", lastAccessTime=" + lastAccessTime +
               ", lastModifiedTime=" + lastModifiedTime +
               ", creationTime=" + creationTime +
               ", method=" + method +
               ", compressedSize=" + compressedSize +
               '}';
    }
}
