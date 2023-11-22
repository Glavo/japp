package org.glavo.japp.boot.jappfs;

import java.io.IOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class JAppDirectoryStream implements DirectoryStream<Path> {

    private final JAppPath path;
    private final JAppFileSystem.DirectoryNode<?> node;
    private final DirectoryStream.Filter<Path> filter;
    private Itr itr;

    private boolean isClosed = false;

    public JAppDirectoryStream(JAppPath path, JAppFileSystem.DirectoryNode<?> node, Filter<Path> filter) {
        this.path = path;
        this.filter = filter;
        this.node = node;
    }

    @Override
    public Iterator<Path> iterator() {
        if (isClosed) {
            throw new ClosedDirectoryStreamException();
        }

        if (itr != null) {
            throw new IllegalStateException("Iterator has already been returned");
        }

        return itr = new Itr();
    }

    @Override
    public void close() throws IOException {
        isClosed = true;
    }

    private final class Itr implements Iterator<Path> {
        private final Iterator<? extends JAppFileSystem.Node> nodeIterator = node.getChildren().iterator();
        private Path nextPath;

        @Override
        public boolean hasNext() {
            if (isClosed) {
                return false;
            }

            if (nextPath != null) {
                return true;
            }

            while (nodeIterator.hasNext()) {
                JAppFileSystem.Node node = nodeIterator.next();
                String fullPath;
                if (path.toString().isEmpty()) {
                    fullPath = node.getName();
                } else if (path.toString().equals("/")) {
                    fullPath = "/" + node.getName();
                } else {
                    fullPath = path + "/" + node.getName();
                }

                JAppPath p = new JAppPath(path.getFileSystem(), fullPath, true);
                try {
                    if (filter == null || filter.accept(p)) {
                        nextPath = p;
                        return true;
                    }
                } catch (IOException ignored) {
                }
            }
            return false;
        }

        @Override
        public Path next() {
            if (hasNext()) {
                Path p = nextPath;
                nextPath = null;
                return p;
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
