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

import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.URLClassPath;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.decompressor.DecompressContext;
import org.glavo.japp.boot.decompressor.classfile.ClassFileDecompressor;
import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.boot.decompressor.zstd.ZstdFrameDecompressor;
import org.glavo.japp.io.ByteBufferInputStream;
import org.glavo.japp.util.ByteBufferUtils;
import org.glavo.japp.io.IOUtils;
import org.glavo.japp.util.XxHash64;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public final class JAppReader implements DecompressContext, Closeable {
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    private static JAppReader systemReader;

    public static JAppReader getSystemReader() {
        if (systemReader == null) {
            throw new IllegalStateException("System JAppReader not initialized");
        }
        return systemReader;
    }

    public static JAppBootArgs openSystemReader(ByteBuffer bootArgs) throws IOException {
        String file = ByteBufferUtils.readString(bootArgs);

        long baseOffset = bootArgs.getLong();
        long metadataOffset = bootArgs.getLong();
        long metadataSize = bootArgs.getLong();

        ZstdFrameDecompressor decompressor = new ZstdFrameDecompressor();

        FileChannel channel = FileChannel.open(Paths.get(file));
        ByteBuffer metadataBuffer = ByteBuffer.allocateDirect(Math.toIntExact(metadataSize)).order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.readFully(channel.position(baseOffset + metadataOffset), metadataBuffer);
        metadataBuffer.flip();
        JAppBootMetadata metadata = JAppBootMetadata.readFrom(metadataBuffer, decompressor);

        ByteBuffer mappedBuffer = null;
        if (metadataOffset < 16 * 1024 * 1024) { // TODO: Configurable threshold
            mappedBuffer = ByteBuffer.allocateDirect((int) metadataOffset);
            IOUtils.readFully(channel.position(baseOffset), mappedBuffer);
            mappedBuffer.flip();
            mappedBuffer = mappedBuffer.asReadOnlyBuffer();
        } else if (metadataOffset < Integer.MAX_VALUE) {
            mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, baseOffset, metadataOffset);
        }

        if (mappedBuffer != null) {
            channel.close();
            channel = null;
        }

        JAppBootArgs args = new JAppBootArgs();
        Map<String, JAppResourceGroup> modules = new HashMap<>();
        Map<String, JAppResourceGroup> classPath = new LinkedHashMap<>();

        int unnamedCount = 0;
        JAppBootArgs.Field field;
        while ((field = JAppBootArgs.Field.readFrom(bootArgs)) != JAppBootArgs.Field.END) {
            switch (field) {
                case MAIN_CLASS: {
                    if (args.mainClass != null) {
                        throw new IOException("Duplicate field: " + field);
                    }
                    args.mainClass = ByteBufferUtils.readString(bootArgs);
                    break;
                }
                case MAIN_MODULE: {
                    if (args.mainModule != null) {
                        throw new IOException("Duplicate field: " + field);
                    }
                    args.mainModule = ByteBufferUtils.readString(bootArgs);
                    break;
                }
                case CLASS_PATH:
                case MODULE_PATH: {
                    boolean isModulePath = field == JAppBootArgs.Field.MODULE_PATH;
                    Map<String, JAppResourceGroup> map;
                    URLClassPath ucp;
                    if (isModulePath) {
                        map = modules;
                        ucp = null;
                    } else {
                        map = classPath;
                        try {
                            ucp = (URLClassPath) MethodHandles.privateLookupIn(BuiltinClassLoader.class, MethodHandles.lookup())
                                    .findGetter(BuiltinClassLoader.class, "ucp", URLClassPath.class)
                                    .invokeExact((BuiltinClassLoader) ClassLoader.getSystemClassLoader());
                        } catch (Throwable e) {
                            throw new AssertionError(e);
                        }
                    }

                    byte type;

                    while ((type = bootArgs.get()) != JAppBootArgs.ID_RESOLVED_REFERENCE_END) {
                        String name = ByteBufferUtils.readStringOrNull(bootArgs);
                        if (type == JAppBootArgs.ID_RESOLVED_REFERENCE_LOCAL) {
                            int index = bootArgs.getInt();
                            JAppResourceGroup group = metadata.getGroups().get(index);
                            if (name != null) {
                                group.initName(name);
                            } else {
                                if (isModulePath) {
                                    throw new IOException("Modules cannot be anonymous");
                                }

                                group.initName("unnamed@" + unnamedCount++);
                            }

                            while ((index = bootArgs.getInt()) != -1) {
                                group.putAll(metadata.getGroups().get(index));
                            }

                            map.put(group.getName(), group);

                            if (!isModulePath) {
                                ucp.addURL(JAppResourceRoot.CLASSPATH.toURI(group).toURL());
                            }
                        } else if (type == JAppBootArgs.ID_RESOLVED_REFERENCE_EXTERNAL) { // External
                            Path path = Paths.get(ByteBufferUtils.readString(bootArgs));

                            if (isModulePath) {
                                args.externalModules.add(path);
                            } else {
                                ucp.addURL(path.toUri().toURL());
                            }
                        } else {
                            throw new IOException();
                        }
                    }

                    break;
                }
                case ADD_READS:
                case ADD_EXPORTS:
                case ADD_OPENS:
                case ENABLE_NATIVE_ACCESS: {
                    List<String> list;
                    if (field == JAppBootArgs.Field.ADD_READS) {
                        list = args.addReads;
                    } else if (field == JAppBootArgs.Field.ADD_EXPORTS) {
                        list = args.addExports;
                    } else if (field == JAppBootArgs.Field.ADD_OPENS) {
                        list = args.addOpens;
                    } else if (field == JAppBootArgs.Field.ENABLE_NATIVE_ACCESS) {
                        list = args.enableNativeAccess;
                    } else {
                        throw new AssertionError("Field: " + field);
                    }
                    ByteBufferUtils.readStringList(bootArgs, list);
                    break;
                }
                default:
                    throw new AssertionError("Field: " + field);
            }
        }

        JAppReader.systemReader = new JAppReader(channel, baseOffset, mappedBuffer, metadata.getPool(), decompressor, modules, classPath);
        return args;
    }

    private final ReentrantLock zstdLock = new ReentrantLock();

    private final FileChannel channel;
    private final long baseOffset;

    private final ByteBuffer mappedBuffer;

    private final Map<String, JAppResourceGroup> modules;
    private final Map<String, JAppResourceGroup> classpath;
    private final Map<String, JAppResourceGroup> resources;

    private final ByteArrayPool pool;
    private final ZstdFrameDecompressor decompressor;

    private volatile boolean isClosed = false;

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
        return !isClosed;
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }

        isClosed = true;
        if (channel != null) {
            channel.close();
        }
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

            while (compressed.hasRemaining()) {
                int n = channel.read(compressed, offset + baseOffset + compressed.position());
                if (n <= 0) {
                    throw new IOException("Unexpected end of file");
                }
            }

            compressed.flip();
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
