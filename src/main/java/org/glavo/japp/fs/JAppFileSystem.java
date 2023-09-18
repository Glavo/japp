package org.glavo.japp.fs;

import org.glavo.japp.JAppFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

public final class JAppFileSystem extends FileSystem {

    private final JAppFileSystemProvider provider;
    private final JAppFile file;
    private final boolean isCloseable;

    private volatile boolean closed = false;

    JAppFileSystem(JAppFileSystemProvider provider, Path jappFile, boolean isCloseable) throws IOException {
        this.provider = provider;
        this.file = new JAppFile(jappFile);
        this.isCloseable = isCloseable;
    }


    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!isCloseable)
            throw new UnsupportedOperationException();

        if (!closed) {
            closed = true;
            file.close();
        }
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return null; // TODO
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return null; // TODO
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return null; // TODO
    }

    @Override
    public Path getPath(String first, String... more) {
        return null; // TODO
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return null; // TODO
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }
}
