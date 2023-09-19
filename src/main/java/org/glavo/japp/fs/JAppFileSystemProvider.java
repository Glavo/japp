package org.glavo.japp.fs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class JAppFileSystemProvider extends FileSystemProvider {
    @Override
    public String getScheme() {
        return "japp";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        return null; // TODO
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        return null; // TODO
    }

    @Override
    public Path getPath(URI uri) {
        return null; // TODO
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return null; // TODO
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null; // TODO
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        // TODO
    }

    @Override
    public void delete(Path path) throws IOException {
        // TODO
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        // TODO
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        // TODO
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false; // TODO
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false; // TODO
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        Objects.requireNonNull(path);
        if (!(path instanceof JAppPath)) {
            throw new ProviderMismatchException();
        }
        return new JAppFileStore((JAppFileSystem) path.getFileSystem());
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        // TODO
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null; // TODO
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return null; // TODO
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return null; // TODO
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        // TODO
    }

    private void checkUri(URI uri) {
        if (!uri.getScheme().equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI does not match this provider");
        }
        if (uri.getAuthority() != null) {
            throw new IllegalArgumentException("Authority component present");
        }
        if (uri.getPath() == null) {
            throw new IllegalArgumentException("Path component is undefined");
        }
        if (!uri.getPath().equals("/")) {
            throw new IllegalArgumentException("Path component should be '/'");
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException("Query component present");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("Fragment component present");
        }
    }
}
