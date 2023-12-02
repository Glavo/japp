package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.decompressor.classfile.ClassFileDecompressor;
import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
import org.glavo.japp.util.ByteBufferInputStream;
import org.glavo.japp.util.IOUtils;
import org.glavo.japp.util.XxHash64;

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

    private final ByteBuffer mappedBuffer;

    private final Map<String, JAppResourceGroup> modules;
    private final Map<String, JAppResourceGroup> classpath;
    private final Map<String, JAppResourceGroup> resources;

    private final ByteArrayPool pool;

    public JAppReader(FileChannel channel, long baseOffset,
                      ByteBuffer mappedBuffer,
                      ByteArrayPool pool,
                      Map<String, JAppResourceGroup> modules, Map<String, JAppResourceGroup> classpath) throws IOException {
        this.channel = channel;
        this.baseOffset = baseOffset;
        this.mappedBuffer = mappedBuffer;
        this.pool = pool;
        this.modules = modules;
        this.classpath = classpath;
        this.resources = new LinkedHashMap<>();
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public ByteArrayPool getPool() {
        return pool;
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

        return g.get(path);
    }

    private void decompressResource(
            CompressionMethod method,
            ByteBuffer compressed,
            byte[] output) throws IOException {
        int size = output.length;

        switch (method) {
            case CLASSFILE: {
                ClassFileDecompressor.decompress(this, compressed, output);
                break;
            }
            case ZSTD: {
                ZstdUtils.decompress(compressed, ByteBuffer.wrap(output, 0, size));
                break;
            }
            default: {
                throw new IOException("Unsupported compression method: " + method);
            }
        }
    }

    private int castArrayLength(long value) {
        if (value > MAX_ARRAY_LENGTH || value < 0) {
            throw new OutOfMemoryError("Value is too large");
        }

        return (int) value;
    }

    public ByteBuffer readResource(JAppResource resource) throws IOException {
        int size = castArrayLength(resource.getSize());
        if (size == 0) {
            return ByteBuffer.allocate(0);
        }

        CompressionMethod method = resource.getMethod();
        int offset = Math.toIntExact(resource.getOffset());
        int compressedSize = castArrayLength(resource.getCompressedSize());

        ByteBuffer compressed;
        if (mappedBuffer != null) {
            compressed = mappedBuffer.duplicate().position(offset).limit(offset + compressedSize).slice();
        } else {
            compressed = ByteBuffer.allocate(compressedSize);

            lock.lock();
            try {
                IOUtils.readFully(channel.position(offset + baseOffset), compressed);
                compressed.flip();
            } finally {
                lock.unlock();
            }
        }

        ByteBuffer uncompressed;
        if (method == CompressionMethod.NONE) {
            uncompressed = compressed;
        } else {
            uncompressed = ByteBuffer.allocate(size);
            decompressResource(method, compressed, uncompressed.array());
        }

        if (resource.needCheck) {
            long checksum = XxHash64.hashByteBufferWithoutUpdate(uncompressed);
            if (resource.checksum != checksum) {
                throw new IOException(String.format(
                        "Failed while verifying resource (expected=%x, actual=%x)",
                        resource.checksum, checksum
                ));
            }

            resource.needCheck = false;
        }

        return uncompressed;
    }

    public InputStream openResource(JAppResource resource) throws IOException {
        return new ByteBufferInputStream(readResource(resource));
    }
}
