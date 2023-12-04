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
import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.boot.JAppResourceRoot;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.regex.Pattern;

public final class JAppFileSystem extends FileSystem {

    private final JAppFileSystemProvider provider;
    private final JAppReader reader;

    private final JAppPath root = new JAppPath(this, "/", true);
    private final List<Path> roots = Collections.singletonList(root);
    private static final Set<String> supportedFileAttributeViews = Collections.singleton("basic");

    JAppFileSystem(JAppFileSystemProvider provider, JAppReader reader) throws IOException {
        this.provider = provider;
        this.reader = reader;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public boolean isOpen() {
        return reader.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    public JAppPath getRootPath() {
        return root;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return roots;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return Collections.singleton(new JAppFileStore(this));
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return supportedFileAttributeViews;
    }

    @Override
    public Path getPath(String first, String... more) {
        if (more.length == 0) {
            return new JAppPath(this, first);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(first);
        for (String path : more) {
            if (!path.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(path);
            }
        }
        return new JAppPath(this, sb.toString());
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        int pos = syntaxAndPattern.indexOf(':');
        if (pos <= 0) {
            throw new IllegalArgumentException();
        }
        String syntax = syntaxAndPattern.substring(0, pos);
        String input = syntaxAndPattern.substring(pos + 1);

        String regex;
        if (syntax.equalsIgnoreCase("glob")) {
            regex = convertGlobToRegex(input);
        } else if (syntax.equalsIgnoreCase("regex")) {
            regex = input;
        } else {
            throw new UnsupportedOperationException("Syntax '" + syntax + "' not recognized");
        }

        final Pattern pattern = Pattern.compile(regex);
        return path -> pattern.matcher(path.toString()).matches();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    // https://stackoverflow.com/a/17369948/7659948
    private static String convertGlobToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append('\\');
                            default:
                                sb.append('\\');
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0)
                        sb.append(".*");
                    else
                        sb.append('*');
                    break;
                case '?':
                    if (inClass == 0)
                        sb.append('.');
                    else
                        sb.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                        sb.append('\\');
                    sb.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i)
                        sb.append('^');
                    else
                        sb.append('!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    if (inGroup > 0)
                        sb.append('|');
                    else
                        sb.append(',');
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static abstract class Node {
        public abstract String getName();
    }

    public static abstract class DirectoryNode<S extends Node> extends Node {
        protected volatile List<S> children;

        public List<S> getChildren() {
            return children;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + (children == null ? "[<unresolved>]" : children);
        }
    }

    public static final class RootNode extends DirectoryNode<ResourceRootNode> {
        public RootNode(List<ResourceRootNode> children) {
            this.children = children;
        }

        @Override
        public String getName() {
            return "";
        }
    }

    public static final class ResourceRootNode extends DirectoryNode<ResourceGroupNode> {
        private final JAppResourceRoot root;

        public ResourceRootNode(JAppResourceRoot root, List<ResourceGroupNode> children) {
            this.root = root;
            this.children = children;
        }

        public JAppResourceRoot getRoot() {
            return root;
        }

        @Override
        public String getName() {
            return root.getRootName();
        }
    }

    public static final class ResourceGroupNode extends DirectoryNode<Node> {
        private final JAppResourceGroup group;

        public ResourceGroupNode(JAppResourceGroup group) {
            this.group = group;
        }

        @Override
        public String getName() {
            return group.getName();
        }

        private static void put(List<Node> root, JAppResource resource) {
            String[] paths = resource.getName().split("/");
            if (paths.length < 1) {
                throw new AssertionError("Resource: " + resource);
            }

            String fileName = paths[paths.length - 1];

            List<Node> current = root;
            for (int i = 0; i < paths.length - 1; i++) {
                String name = paths[i];
                SubDirectoryNode dir = null;

                for (Node node : current) {
                    if (node.getName().equals(name)) {
                        if (!(node instanceof SubDirectoryNode)) {
                            throw new AssertionError();
                        }

                        dir = (SubDirectoryNode) node;
                        break;
                    }
                }

                if (dir == null) {
                    dir = new SubDirectoryNode(name);
                    current.add(dir);
                }

                current = dir.children;
            }

            current.add(new ResourceNode(fileName, resource));
        }

        private List<Node> resolve() {
            List<Node> list = new ArrayList<>();

            for (JAppResource resource : group.values()) {
                put(list, resource);
            }

            return list;
        }

        @Override
        public List<Node> getChildren() {
            if (this.children != null) {
                return this.children;
            }

            synchronized (this) {
                if (this.children != null) {
                    return this.children;
                }

                return this.children = resolve();
            }
        }
    }

    public static final class SubDirectoryNode extends DirectoryNode<Node> {
        private final String name;

        public SubDirectoryNode(String name) {
            this.name = name;
            this.children = new ArrayList<>();
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static final class ResourceNode extends Node {
        private final String name;
        private final JAppResource resource;

        public ResourceNode(String name, JAppResource resource) {
            this.resource = resource;
            this.name = name;
        }

        public JAppResource getResource() {
            return resource;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "ResourceNode[" + resource + "]";
        }
    }
}
