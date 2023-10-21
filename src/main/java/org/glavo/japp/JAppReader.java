package org.glavo.japp;

import org.glavo.japp.util.IOUtils;
import org.glavo.japp.thirdparty.json.JSONArray;
import org.glavo.japp.thirdparty.json.JSONObject;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class JAppReader implements Closeable {
    private static final class SystemReaderHolder {
        static final JAppReader READER;
        static final Throwable EXCEPTION;

        static {
            String property = System.getProperty("org.glavo.japp.file");

            JAppReader reader = null;
            Throwable exception = null;
            if (property != null) {
                try {
                    reader = new JAppReader(Paths.get(property), JAppRuntimeContext.fromCurrentEnvironment());
                } catch (IOException e) {
                    exception = e;
                }
            }
            READER = reader;
            EXCEPTION = exception;
        }
    }

    public static final short MAJOR_VERSION = -1;
    public static final short MINOR_VERSION = 0;

    public static final int FILE_END_SIZE = 48;

    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    public static JAppReader getSystemReader() {
        if (SystemReaderHolder.READER == null) {
            throw new IllegalStateException("No System JAppReader", SystemReaderHolder.EXCEPTION);
        }

        return SystemReaderHolder.READER;
    }

    private final ReentrantLock lock = new ReentrantLock();

    private final FileChannel channel;

    private final long contentOffset;

    private final JAppMetadata metadata;

    private final JAppRuntimeContext context;

    public JAppReader(Path file, JAppRuntimeContext context) throws IOException {
        this.context = context;
        this.channel = FileChannel.open(file);

        try {
            long fileSize = channel.size();

            if (fileSize < FILE_END_SIZE) {
                throw new IOException("File is too small");
            }

            int endBufferSize = (int) Math.min(fileSize, 8192);
            ByteBuffer endBuffer = ByteBuffer.allocate(endBufferSize).order(ByteOrder.LITTLE_ENDIAN);

            channel.position(fileSize - endBufferSize);

            IOUtils.readFully(channel, endBuffer);

            endBuffer.limit(endBufferSize).position(endBufferSize - FILE_END_SIZE);

            int magicNumber = endBuffer.getInt();
            if (magicNumber != 0x5050414a) {
                throw new IOException("Invalid magic number: " + Long.toHexString(magicNumber));
            }

            short majorVersion = endBuffer.getShort();
            short minorVersion = endBuffer.getShort();

            if (majorVersion != MAJOR_VERSION || minorVersion != MINOR_VERSION) {
                throw new IOException("Version number mismatch");
            }

            long flags = endBuffer.getLong();

            long fileContentSize = endBuffer.getLong();
            long metadataOffset = endBuffer.getLong();

            assert endBuffer.remaining() == 16;

            if (flags != 0) {
                throw new IOException("Unsupported flags: " + Long.toBinaryString(flags));
            }

            if (fileContentSize > fileSize || fileContentSize < FILE_END_SIZE) {
                throw new IOException("Invalid file size: " + fileContentSize);
            }

            if (metadataOffset >= fileContentSize - FILE_END_SIZE) {
                throw new IOException("Invalid metadata offset: " + metadataOffset);
            }

            this.contentOffset = fileSize - fileContentSize;

            long metadataSize = fileContentSize - FILE_END_SIZE - metadataOffset;

            String json;
            if (metadataSize < endBufferSize - FILE_END_SIZE) {
                json = new String(endBuffer.array(), (int) (endBufferSize - metadataSize - FILE_END_SIZE), (int) metadataSize, UTF_8);
            } else {
                if (metadataSize > (1 << 30)) {
                    throw new IOException("Metadata is too large");
                }

                ByteBuffer metadataBuffer = ByteBuffer.allocate((int) metadataSize);
                channel.position(contentOffset + metadataOffset);
                IOUtils.readFully(channel, metadataBuffer);

                json = new String(metadataBuffer.array(), UTF_8);
            }


            this.metadata = JAppMetadata.fromJson(new JSONObject(json), context);
        } catch (Throwable e) {
            try {
                channel.close();
            } catch (Throwable e2) {
                e2.addSuppressed(e);
                throw e2;
            }

            throw e;
        }
    }

    public void ensureResolved() {
        if (context == null) {
            throw new IllegalStateException();
        }
    }

    public JAppMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public JAppResource findResource(boolean isModulePath, String itemName, String path) {
        ensureResolved();

        JAppClasspathItem item = isModulePath ? metadata.getModulePathItems().get(itemName) : metadata.getClassPathItems().get(itemName);
        if (item == null) {
            return null;
        }
        return item.getResources().get(path);
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
            IOUtils.readFully(channel.position(offset + contentOffset), ByteBuffer.wrap(array));
        } finally {
            lock.unlock();
        }

        return array;
    }

    public InputStream getResourceAsInputStream(JAppResource resource) throws IOException {
        return new ByteArrayInputStream(getResourceAsByteArray(resource));
    }
}
