package org.glavo.japp.boot.fs;

import org.glavo.japp.TODO;

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Objects;

public final class JAppPath implements Path {

    private final JAppFileSystem fs;
    private final String path;

    private String[] pathElements;

    JAppPath(JAppFileSystem fs, String path) {
        this.fs = fs;
        this.path = normalize(path);
    }

    JAppPath(JAppFileSystem fs, String path, boolean normalized) {
        this.fs = fs;
        this.path = normalized ? path : normalize(path);
    }

    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    @Override
    public boolean isAbsolute() {
        return path.startsWith("/");
    }

    @Override
    public Path getRoot() {
        if (this.isAbsolute()) {
            return fs.getRootPath();
        } else {
            return null;
        }
    }

    @Override
    public Path getFileName() {
        if (path.isEmpty()) {
            return this;
        }

        int off = path.lastIndexOf('/');
        if (off == -1) {
            return this;
        }
        if (off == path.length() - 1) {
            return null;
        }

        return new JAppPath(fs, path.substring(off + 1));
    }

    @Override
    public Path getParent() {


        return null;
    }

    private String[] getPathElements() {
        if (pathElements == null) {
            pathElements = path.split("/");
        }

        return pathElements;
    }

    @Override
    public int getNameCount() {
        return getPathElements().length;
    }

    @Override
    public Path getName(int index) {
        try {
            return new JAppPath(fs, getPathElements()[index]);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        String[] elements = getPathElements();
        if (beginIndex < 0 || endIndex > elements.length || beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }

        if (beginIndex == 0 && endIndex == elements.length) {
            return this;
        }

        String newPath;
        if (endIndex - beginIndex == 1) {
            newPath = elements[beginIndex];
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(beginIndex);
            for (int i = beginIndex + 1; i < endIndex; i++) {
                builder.append('/').append(elements[i]);
            }
            newPath = builder.toString();
        }
        return new JAppPath(fs, newPath, true);
    }

    @Override
    public boolean startsWith(Path other) {
        if (!(other instanceof JAppPath)) {
            return false;
        }

        final JAppPath o = (JAppPath) other;
        if (isAbsolute() != o.isAbsolute() || !this.path.startsWith(o.path)) {
            return false;
        }
        int otherLength = o.path.length();
        if (otherLength == 0) {
            return this.path.isEmpty();
        }
        // check match is on name boundary
        return this.path.length() == otherLength || this.path.charAt(otherLength) == '/';
    }

    @Override
    public boolean endsWith(Path other) {
        if (!(other instanceof JAppPath)) {
            return false;
        }

        if (other.isAbsolute()) {
            return this.equals(other);
        }

        // fixme: Maybe it's implemented wrong, I'll look at it later

        String[] thisElements = this.getPathElements();
        String[] otherElements = ((JAppPath) other).getPathElements();

        if (thisElements.length < otherElements.length) {
            return false;
        }


        int n = thisElements.length - otherElements.length;
        for (int i = 0; i < otherElements.length; i++) {
            if (!thisElements[n + i].equals(otherElements[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Path normalize() {
        throw new TODO();
    }

    @Override
    public Path resolve(Path other) {
        final JAppPath o = checkPath(other);
        if (this.path.isEmpty() || o.isAbsolute()) {
            return o;
        }
        if (o.path.isEmpty()) {
            return this;
        }
        return new JAppPath(fs, path + '/' + o.path, true);
    }

    @Override
    public Path relativize(Path other) {
        throw new TODO();
    }

    @Override
    public URI toUri() {
        try {
            return new URI("japp", null, path, null);
        } catch (URISyntaxException e) {
            throw new IOError(e);
        }
    }

    @Override
    public Path toAbsolutePath() {
        if (isAbsolute())
            return this;
        return new JAppPath(fs, "/" + path);
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        throw new TODO();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        Objects.requireNonNull(watcher, "watcher");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(modifiers, "modifiers");
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Path other) {
        return this.path.compareTo(checkPath(other).path);
    }

    @Override
    public String toString() {
        return path;
    }

    // Helper

    private JAppPath checkPath(Path path) {
        Objects.requireNonNull(path);
        if (path instanceof JAppPath) {
            return (JAppPath) path;
        }

        throw new ProviderMismatchException("path class: " + path.getClass());
    }

    private static String normalize(String path) {
        if (path.isEmpty()) {
            return path;
        }
        int i;
        char prevCh = 0;
        for (i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\\') {
                break;
            }
            if (c == '/' && prevCh == '/') {
                break;
            }
            prevCh = c;
        }

        if (i == path.length()) {
            if (prevCh == '/' && path.length() > 1) {
                return path.substring(0, path.length() - 1);
            } else {
                return path;
            }
        }

        StringBuilder res = new StringBuilder(path.length());
        res.append(path, 0, i);
        for (; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\\') {
                c = '/';
            }
            if (c == '/' && prevCh == '/') {
                continue;
            }
            res.append(c);
            prevCh = c;
        }
        if (prevCh == '/' && res.length() > 1) {
            res.deleteCharAt(res.length() - 1);
        }
        return res.toString();
    }

}
