/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.boot.jappfs;

import org.glavo.japp.boot.JAppReader;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.util.ByteBufferChannel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.StandardOpenOption.*;

public final class JAppFileSystemProvider extends FileSystemProvider {

    private final JAppFileSystem fileSystem;
    private final ReentrantLock lock = new ReentrantLock();

    public JAppFileSystemProvider() throws IOException {
        this(JAppReader.getSystemReader());
    }

    public JAppFileSystemProvider(JAppReader reader) throws IOException {
        this.fileSystem = new JAppFileSystem(this, reader);
    }

    @Override
    public String getScheme() {
        return "japp";
    }

    public JAppFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        throw new FileSystemAlreadyExistsException();
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        checkUri(uri);
        return fileSystem;
    }

    @Override
    public Path getPath(URI uri) {
        String path = uri.getPath();
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException();
        }
        return fileSystem.getPath(path);
    }

    private static void checkOptions(Set<? extends OpenOption> options) {
        for (OpenOption option : options) {
            if (option == null) {
                throw new NullPointerException();
            }
            if (!(option instanceof StandardOpenOption)) {
                throw new IllegalArgumentException();
            }
        }
        if (options.contains(WRITE) || options.contains(APPEND)) {
            throw new ReadOnlyFileSystemException();
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        checkOptions(options);

        JAppPath jappPath = toJAppPath(path);
        JAppFileSystem fs = jappPath.getFileSystem();
        JAppFileSystem.Node node = fs.resolve(jappPath);
        if (node == null) {
            throw new FileNotFoundException(path.toString());
        }

        if (node instanceof JAppFileSystem.DirectoryNode) {
            throw new FileSystemException(path + " is a directory");
        }

        JAppResource resource = ((JAppFileSystem.ResourceNode) node).getResource();
        return new ByteBufferChannel(fs.reader.readResource(resource));
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        JAppPath jappPath = toJAppPath(dir);

        return null; // TODO
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        JAppPath jappPath = toJAppPath(dir);
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void delete(Path path) throws IOException {
        JAppPath jappPath = toJAppPath(path);
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        JAppPath jappSource = toJAppPath(source);
        JAppPath jappTarget = toJAppPath(target);
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        JAppPath jappSource = toJAppPath(source);
        JAppPath jappTarget = toJAppPath(target);
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public boolean isSameFile(Path path1, Path path2) throws IOException {
        JAppPath jappPath1 = toJAppPath(path1);
        JAppPath jappPath2 = toJAppPath(path2);
        return jappPath1.toRealPath().equals(jappPath2.toRealPath());
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return new JAppFileStore(toJAppPath(path).getFileSystem());
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        JAppPath jappPath = toJAppPath(path);

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
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        JAppPath jappPath = toJAppPath(path);
        if (type == BasicFileAttributeView.class) {
            return (V) new JAppFileAttributeView(jappPath, false, options);
        }
        if (type == JAppFileAttributeView.class) {
            return (V) new JAppFileAttributeView(jappPath, true, options);
        }
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (type == BasicFileAttributes.class || type == JAppFileAttributes.class) {
            JAppPath jappPath = toJAppPath(path);
            JAppFileSystem fileSystem = jappPath.getFileSystem();
            JAppFileSystem.Node node = fileSystem.resolve(jappPath);
            if (node == null) {
                throw new NoSuchFileException(path.toString());
            }
        }
        return null;
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

}
