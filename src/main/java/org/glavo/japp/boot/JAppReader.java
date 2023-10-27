package org.glavo.japp.boot;

import org.glavo.japp.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public final class JAppReader implements Closeable {
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    private static JAppReader reader;

    public static void initSystemReader(JAppReader reader) {
        if (JAppReader.reader != null) {
            throw new IllegalStateException("System JAppReader has been initialized");
        }

        JAppReader.reader = reader;
    }

    public static JAppReader getSystemReader() {
        if (reader == null) {
            throw new IllegalStateException("System JAppReader not initialized");
        }
        return reader;
    }

    private final ReentrantLock lock = new ReentrantLock();

    private final FileChannel channel;
    private final long baseOffset;

    private final Map<String, JAppResourceGroup> modules;
    private final Map<String, JAppResourceGroup> classpath;
    private final Map<String, JAppResourceGroup> resources;

    public JAppReader(FileChannel channel, long baseOffset, Map<String, JAppResourceGroup> modules, Map<String, JAppResourceGroup> classpath) throws IOException {
        this.channel = channel;
        this.baseOffset = baseOffset;
        this.modules = modules;
        this.classpath = classpath;
        this.resources = new LinkedHashMap<>();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public Map<String, JAppResourceGroup> getRoot(JAppResourceRoot root) {
        switch (root) {
            case MODULES:
                return modules;
            case CLASSPATH:
                return classpath;
            case RESOURCE:
                return resources;
            default:
                throw new AssertionError(root);
        }
    }

    public JAppResource findResource(JAppResourceRoot root, String group, String path) {
        JAppResourceGroup g = getRoot(root).get(group);
        if (g == null) {
            return null;
        }

        return g.getResources().get(path);
    }

    public byte[] getResourceAsByteArray(JAppResource resource) throws IOException {
        long offset = resource.getOffset();
        long size = resource.getSize();

        if (size > MAX_ARRAY_LENGTH || size < 0) {
            throw new OutOfMemoryError("Resource is too large");
        }

        byte[] array = new byte[(int) size];
        if (size == 0) {
            return array;
        }

        lock.lock();
        try {
            IOUtils.readFully(channel.position(offset + baseOffset), ByteBuffer.wrap(array));
        } finally {
            lock.unlock();
        }

        return array;
    }

    public InputStream getResourceAsInputStream(JAppResource resource) throws IOException {
        return new ByteArrayInputStream(getResourceAsByteArray(resource));
    }
}
