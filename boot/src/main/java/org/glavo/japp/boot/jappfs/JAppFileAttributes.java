package org.glavo.japp.boot.jappfs;

import org.glavo.japp.TODO;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class JAppFileAttributes implements BasicFileAttributes {
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
