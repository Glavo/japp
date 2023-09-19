package org.glavo.japp.fs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Objects;

public final class JAppPath implements Path {

    private final JAppFileSystem fs;
    private final String path;

    JAppPath(JAppFileSystem fs, String path) {
        this.fs = fs;
        this.path = normalize(path);
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
        return null;
    }

    @Override
    public Path getFileName() {
        return null;
    }

    @Override
    public Path getParent() {
        return null;
    }

    @Override
    public int getNameCount() {
        return 0;
    }

    @Override
    public Path getName(int index) {
        return null;
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return null;
    }

    @Override
    public boolean startsWith(Path other) {
        return false;
    }

    @Override
    public boolean endsWith(Path other) {
        return false;
    }

    @Override
    public Path normalize() {
        return null;
    }

    @Override
    public Path resolve(Path other) {
        return null;
    }

    @Override
    public Path relativize(Path other) {
        return null;
    }

    @Override
    public URI toUri() {
        return null;
    }

    @Override
    public Path toAbsolutePath() {
        return null;
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return null;
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
