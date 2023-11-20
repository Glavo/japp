package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.TODO;
import org.glavo.japp.boot.decompressor.classfile.ClassFileDecompressor;
import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.boot.decompressor.lz4.LZ4Decompressor;
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
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
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

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

    private final ByteArrayPool pool;

    public JAppReader(FileChannel channel, long baseOffset,
                      ByteArrayPool pool,
                      Map<String, JAppResourceGroup> modules, Map<String, JAppResourceGroup> classpath) throws IOException {
        this.channel = channel;
        this.baseOffset = baseOffset;
        this.pool = pool;
        this.modules = modules;
        this.classpath = classpath;
        this.resources = new LinkedHashMap<>();
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

        return g.getResources().get(path);
    }

    private void getResourceAsByteArrayImpl(
            CompressionMethod method,
            long offset,
            int size, int compressedSize,
            byte[] output) throws IOException {
        if (method == CompressionMethod.NONE) {
            IOUtils.readFully(channel.position(offset + baseOffset), ByteBuffer.wrap(output));
            return;
        }

        ByteBuffer compressed = ByteBuffer.allocate(compressedSize);
        IOUtils.readFully(channel.position(offset + baseOffset), compressed);
        compressed.flip();

        switch (method) {
            case CLASSFILE: {
                ClassFileDecompressor.decompress(this, compressed, output);
                break;
            }
            case DEFLATE: {
                Inflater inflater = new Inflater();
                inflater.setInput(compressed.array());

                try {
                    int count = 0;
                    while (count < size) {
                        if (inflater.finished()) {
                            throw new IOException("Unexpected end of data");
                        }
                        count += inflater.inflate(output);
                    }
                } catch (DataFormatException e) {
                    throw new IOException(e);
                } finally {
                    inflater.end();
                }
                break;
            }
            case LZ4: {
                LZ4Decompressor.decompress(compressed.array(), output);
                break;
            }
            case ZSTD: {
                ZstdUtils.decompress(compressed.array(), 0, compressedSize, output, 0, size);
                break;
            }
            default: {
                throw new TODO("Method: " + method);
            }
        }
    }

    private int castArrayLength(long value) {
        if (value > MAX_ARRAY_LENGTH || value < 0) {
            throw new OutOfMemoryError("Value is too large");
        }

        return (int) value;
    }

    public byte[] getResourceAsByteArray(JAppResource resource) throws IOException {
        int size = castArrayLength(resource.getSize());

        byte[] array = new byte[(int) size];
        if (size == 0) {
            return array;
        }

        CompressionMethod method = resource.getMethod();
        long offset = resource.getOffset();
        int compressedSize = castArrayLength(resource.getCompressedSize());

        lock.lock();
        try {
            getResourceAsByteArrayImpl(method, offset, size, compressedSize, array);
        } catch (Throwable e) {
            e.printStackTrace(); // TODO: DEBUG
        } finally {
            lock.unlock();
        }

        return array;
    }

    public InputStream getResourceAsInputStream(JAppResource resource) throws IOException {
        return new ByteArrayInputStream(getResourceAsByteArray(resource));
    }
}
