package org.glavo.japp.fs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public final class JAppFileSystemProvider extends FileSystemProvider {

    private final Map<Path, JAppFileSystem> filesystems = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    private static Path uriToPath(URI uri) {
        if (!"japp".equals(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme is not 'japp'");
        }
        try {
            String spec = uri.getRawSchemeSpecificPart();
            int sep = spec.indexOf("!/");
            if (sep != -1) {
                spec = spec.substring(0, sep);
            }
            return Paths.get(new URI(spec)).toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static JAppPath toJAppPath(Path path) {
        Objects.requireNonNull(path);
        if (path instanceof JAppPath) {
            return (JAppPath) path;
        }
        throw new ProviderMismatchException(path.toString());
    }

    @Override
    public String getScheme() {
        return "japp";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        Path path = uriToPath(uri);
        Path realPath = path.toRealPath();
        if (!Files.isRegularFile(path)) {
            throw new UnsupportedOperationException();
        }

        lock.lock();
        try {
            if (filesystems.containsKey(realPath)) {
                throw new FileSystemAlreadyExistsException(realPath.toString());
            }

            JAppFileSystem fs = new JAppFileSystem(this, path, env);
            filesystems.put(realPath, fs);
            return fs;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        Path realPath;
        try {
            realPath = uriToPath(uri).toRealPath();
        } catch (IOException e) {
            FileSystemNotFoundException ex = new FileSystemNotFoundException(uri.toString());
            ex.addSuppressed(e);
            throw ex;
        }

        JAppFileSystem fs;
        lock.lock();
        try {
            fs = filesystems.get(realPath);
        } finally {
            lock.unlock();
        }

        if (fs == null) {
            throw new FileSystemNotFoundException(uri.toString());
        }
        return fs;
    }

    @Override
    public Path getPath(URI uri) {
        String spec = uri.getSchemeSpecificPart();
        int sep = spec.indexOf("!/");
        if (sep == -1)
            throw new IllegalArgumentException("URI: " + uri + " does not contain japp path info");
        return getFileSystem(uri).getPath(spec.substring(sep + 1));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        JAppPath jappPath = toJAppPath(path);
        return null; // TODO
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        JAppPath jappPath = toJAppPath(dir);
        return null; // TODO
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        JAppPath jappPath = toJAppPath(dir);
        // TODO
    }

    @Override
    public void delete(Path path) throws IOException {
        JAppPath jappPath = toJAppPath(path);
        // TODO
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        JAppPath jappSource = toJAppPath(source);
        JAppPath jappTarget = toJAppPath(target);
        // TODO
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        JAppPath jappSource = toJAppPath(source);
        JAppPath jappTarget = toJAppPath(target);
        // TODO
    }

    @Override
    public boolean isSameFile(Path path1, Path path2) throws IOException {
        JAppPath jappPath1 = toJAppPath(path1);
        JAppPath jappPath2 = toJAppPath(path2);
        return false; // TODO
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
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
        JAppPath jappPath = toJAppPath(path);

        boolean w = false;
        boolean x = false;
        for (AccessMode mode : modes) {
            switch (mode) {
                case READ:
                    break;
                case WRITE:
                case EXECUTE:
                    throw new AccessDeniedException(jappPath.toString());
                default:
                    throw new UnsupportedOperationException();
            }
        }

        if (!exists(jappPath)) {
            throw new NoSuchFileException(path.toString());
        }
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
        toJAppPath(path);
        throw new ReadOnlyFileSystemException();
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
