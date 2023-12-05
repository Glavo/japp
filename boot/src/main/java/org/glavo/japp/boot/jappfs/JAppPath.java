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

import org.glavo.japp.TODO;

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JAppPath implements Path {

    private final JAppFileSystem fileSystem;
    private final String path;

    private String[] pathElements;

    JAppPath(JAppFileSystem fileSystem, String path) {
        this.fileSystem = fileSystem;
        this.path = normalize(path);
    }

    JAppPath(JAppFileSystem fileSystem, String path, boolean normalized) {
        this.fileSystem = fileSystem;
        this.path = normalized ? path : normalize(path);
    }

    @Override
    public JAppFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return path.startsWith("/");
    }

    @Override
    public Path getRoot() {
        if (this.isAbsolute()) {
            return fileSystem.getRootPath();
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

        return new JAppPath(fileSystem, path.substring(off + 1), true);
    }

    @Override
    public Path getParent() {
        if (path.isEmpty() || path.equals("/")) {
            return null;
        }

        int off = path.lastIndexOf('/');
        if (off < 0) {
            return null;
        }

        if (off == 0) {
            return fileSystem.getRootPath();
        }

        return new JAppPath(fileSystem, path.substring(0, off), true);
    }

    String[] getPathElements() {
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
            return new JAppPath(fileSystem, getPathElements()[index]);
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
        return new JAppPath(fileSystem, newPath, true);
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
        int len = path.length();
        if (len == 0 || (!path.contains("./") && path.charAt(len - 1) != '.')) {
            return this;
        }

        List<String> list = new ArrayList<>();
        String[] elements = getPathElements();

        for (String element : elements) {
            if (element.equals(".")) {
                // ignored
            } else if (element.equals("..")) {
                int lastIndex = list.size() - 1;
                if (lastIndex >= 0) {
                    list.remove(lastIndex);
                }
            } else {
                list.add(element);
            }
        }

        if (list.isEmpty()) {
            return this.isAbsolute() ? fileSystem.getRootPath() : new JAppPath(fileSystem, "", true);
        }

        StringBuilder res = new StringBuilder(path.length());
        if (this.isAbsolute()) {
            res.append('/');
        }
        res.append(list.get(0));

        for (int i = 1; i < list.size(); i++) {
            res.append('/');
            res.append(list.get(i));
        }

        return new JAppPath(fileSystem, res.toString(), true);
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
        return new JAppPath(fileSystem, path + '/' + o.path, true);
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
        if (isAbsolute()) {
            return this;
        }
        return new JAppPath(fileSystem, "/" + path, true);
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return toAbsolutePath().normalize();
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
