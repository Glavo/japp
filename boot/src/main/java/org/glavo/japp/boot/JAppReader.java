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
package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.decompressor.DecompressContext;
import org.glavo.japp.boot.decompressor.classfile.ClassFileDecompressor;
import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.boot.decompressor.zstd.ZstdFrameDecompressor;
import org.glavo.japp.util.ByteBufferInputStream;
import org.glavo.japp.util.ByteBufferUtils;
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

public final class JAppReader implements DecompressContext, Closeable {
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

    private final ReentrantLock fileLock = new ReentrantLock();
    private final ReentrantLock zstdLock = new ReentrantLock();

    private final FileChannel channel;
    private final long baseOffset;

    private final ByteBuffer mappedBuffer;

    private final Map<String, JAppResourceGroup> modules;
    private final Map<String, JAppResourceGroup> classpath;
    private final Map<String, JAppResourceGroup> resources;

    private final ByteArrayPool pool;
    private final ZstdFrameDecompressor decompressor;

    public JAppReader(FileChannel channel, long baseOffset,
                      ByteBuffer mappedBuffer,
                      ByteArrayPool pool,
                      ZstdFrameDecompressor decompressor,
                      Map<String, JAppResourceGroup> modules,
                      Map<String, JAppResourceGroup> classpath) throws IOException {
        this.channel = channel;
        this.baseOffset = baseOffset;
        this.mappedBuffer = mappedBuffer;
        this.pool = pool;
        this.decompressor = decompressor;
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

    @Override
    public ByteArrayPool getPool() {
        return pool;
    }

    @Override
    public void decompressZstd(ByteBuffer input, ByteBuffer output) {
        zstdLock.lock();
        try {
            decompressor.decompress(input, output);
        } finally {
            zstdLock.unlock();
        }
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

    private ByteBuffer decompressResource(
            CompressionMethod method,
            ByteBuffer compressed,
            int size) throws IOException {

        byte[] output = new byte[size];
        ByteBuffer outputBuffer = ByteBuffer.wrap(output);

        switch (method) {
            case CLASSFILE: {
                ClassFileDecompressor.decompress(this, compressed, output);
                break;
            }
            case ZSTD: {
                decompressZstd(compressed, outputBuffer);
                outputBuffer.flip();
                break;
            }
            default: {
                throw new IOException("Unsupported compression method: " + method);
            }
        }

        return outputBuffer;
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
            compressed = ByteBufferUtils.slice(mappedBuffer, offset, compressedSize);
        } else {
            compressed = ByteBuffer.allocate(compressedSize);

            fileLock.lock();
            try {
                IOUtils.readFully(channel.position(offset + baseOffset), compressed);
                compressed.flip();
            } finally {
                fileLock.unlock();
            }
        }

        ByteBuffer uncompressed = method == CompressionMethod.NONE ? compressed : decompressResource(method, compressed, size);

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
