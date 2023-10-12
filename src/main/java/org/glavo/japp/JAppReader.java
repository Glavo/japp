package org.glavo.japp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class JAppReader implements Closeable {
    private static final class SystemReaderHolder {
        static final JAppReader READER;

        static {
            String property = System.getProperty("org.glavo.japp.file");

            JAppReader reader = null;
            if (property != null) {
                try {
                    reader = new JAppReader(Paths.get(property));
                } catch (IOException ignored) {
                }
            }
            READER = reader;
        }
    }

    public static final short MAJOR_VERSION = -1;
    public static final short MINOR_VERSION = 0;

    public static final int FILE_END_SIZE = 48;

    public static JAppReader getSystemReader() {
        return SystemReaderHolder.READER;
    }

    private final ReentrantLock lock = new ReentrantLock();

    private final FileChannel channel;

    private final long contentOffset;

    private final List<JAppClasspathItem> modulePath = new ArrayList<>();
    private final List<JAppClasspathItem> classPath = new ArrayList<>();

    private final List<String> addReads = new ArrayList<>();
    private final List<String> addExports = new ArrayList<>();
    private final List<String> addOpens = new ArrayList<>();

    private final String mainClass;
    private final String mainModule;

    public JAppReader(Path file) throws IOException {
        this.channel = FileChannel.open(file);

        try {
            long fileSize = channel.size();

            if (fileSize < FILE_END_SIZE) {
                throw new IOException("File is too small");
            }

            int endBufferSize = (int) Math.min(fileSize, 8192);
            ByteBuffer endBuffer = ByteBuffer.allocate(endBufferSize).order(ByteOrder.LITTLE_ENDIAN);

            channel.position(fileSize - endBufferSize);

            while (channel.read(endBuffer) > 0) {
            }

            if (endBuffer.remaining() > 0) {
                throw new EOFException();
            }

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
                while (channel.read(metadataBuffer) > 0) {
                }

                if (metadataBuffer.remaining() > 0) {
                    throw new EOFException();
                }

                json = new String(metadataBuffer.array(), UTF_8);
            }

            JSONObject obj = new JSONObject(json);

            readClasspathItems(classPath, obj.optJSONArray("Class-Path"));
            readClasspathItems(modulePath, obj.optJSONArray("Module-Path"));

            readJsonArray(addReads, obj, "Add-Reads" );
            readJsonArray(addExports, obj, "Add-Exports");
            readJsonArray(addOpens, obj, "Add-Opens");

            mainClass = obj.optString("Main-Class");
            mainModule = obj.optString("Main-Module");
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

    private static void readClasspathItems(List<JAppClasspathItem> list, JSONArray array) {
        if (array == null) {
            return;
        }

        for (Object item : array) {
            list.add(JAppClasspathItem.fromJson((JSONObject) item));
        }
    }

    private static void readJsonArray(List<String> list, JSONObject obj, String key) {
        JSONArray arr = obj.optJSONArray(key);
        if (arr == null) {
            return;
        }

        for (Object o : arr) {
            list.add((String) o);
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public InputStream getInputStream(JAppResource entry) throws IOException {
        // TODO: Need optimization
        return new InputStream() {
            private long count = 0;

            @Override
            public int read() throws IOException {
                if (count >= entry.size) {
                    return -1;
                }

                lock.lock();
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(1);
                    channel.position(entry.offset + count);

                    if (channel.read(buffer) != 1) {
                        return -1;
                    } else {
                        count++;
                        return buffer.array()[0] & 0xff;
                    }
                } finally {
                    lock.unlock();
                }
            }
        };
    }
}
