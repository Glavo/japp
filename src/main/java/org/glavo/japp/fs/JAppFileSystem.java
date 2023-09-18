package org.glavo.japp.fs;

import com.hrakaroo.glob.GlobPattern;
import com.hrakaroo.glob.MatchingEngine;
import org.glavo.japp.JAppFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;
import java.util.regex.Pattern;

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
        int pos = syntaxAndPattern.indexOf(':');
        if (pos <= 0) {
            throw new IllegalArgumentException();
        }
        String syntax = syntaxAndPattern.substring(0, pos);
        String input = syntaxAndPattern.substring(pos + 1);
        if (syntax.equalsIgnoreCase("glob")) {
            final MatchingEngine glob = GlobPattern.compile(input);
            return path -> glob.matches(path.toString());
        } else if (syntax.equalsIgnoreCase("regex")) {
            final Pattern pattern = Pattern.compile(input);
            return path -> pattern.matcher(path.toString()).matches();
        } else {
            throw new UnsupportedOperationException("Syntax '" + syntax + "' not recognized");
        }
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
