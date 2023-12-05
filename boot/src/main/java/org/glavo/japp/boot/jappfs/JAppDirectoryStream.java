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

import java.io.IOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class JAppDirectoryStream implements DirectoryStream<Path> {

    private final JAppPath path;
    private final JAppFileSystem.DirectoryNode<?> node;
    private final DirectoryStream.Filter<? super Path> filter;
    private Itr itr;

    private boolean isClosed = false;

    public JAppDirectoryStream(JAppPath path, JAppFileSystem.DirectoryNode<?> node, Filter<? super Path> filter) {
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
                JAppPath p = (JAppPath) path.resolve(node.getName());
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
