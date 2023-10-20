package org.glavo.japp;

import org.glavo.japp.condition.ConditionalHandler;
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
                    reader = new JAppReader(Paths.get(property), ConditionalHandler.fromCurrentEnvironment());
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

    private final Map<String, JAppClasspathItem> modulePath = new LinkedHashMap<>();
    private final Map<String, JAppClasspathItem> classPath = new LinkedHashMap<>();

    private final List<String> jvmProperties = new ArrayList<>();
    private final List<String> addReads = new ArrayList<>();
    private final List<String> addExports = new ArrayList<>();
    private final List<String> addOpens = new ArrayList<>();

    private final String mainClass;
    private final String mainModule;

    private final ConditionalHandler conditionalHandler;

    public JAppReader(Path file, ConditionalHandler conditionalHandler) throws IOException {
        this.conditionalHandler = conditionalHandler;
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

            JSONObject obj = new JSONObject(json);

            readClasspathItems(false, obj.optJSONArray("Class-Path"));
            readClasspathItems(true, obj.optJSONArray("Module-Path"));

            readJsonArray(jvmProperties, obj, "Properties");
            readJsonArray(addReads, obj, "Add-Reads");
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

    private void readClasspathItems(boolean isModulePath, JSONArray array) throws IOException {
        Map<String, JAppClasspathItem> map = isModulePath ? this.modulePath : this.classPath;

        if (array != null) {
            for (Object jsonItem : array) {
                JAppClasspathItem item = JAppClasspathItem.fromJson(((JSONObject) jsonItem), conditionalHandler);
                String name = item.getName();

                if (name == null) {
                    throw new IOException("Item missing name");
                }

                if (map.put(name, item) != null) {
                    throw new IOException(String.format("Duplicate %s path item: %s", isModulePath ? "module" : "", name));
                }
            }
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

    public void ensureResolved() {
        if (conditionalHandler == null) {
            throw new IllegalStateException();
        }
    }

    public List<String> getJvmProperties() {
        return jvmProperties;
    }

    public List<String> getAddReads() {
        return addReads;
    }

    public List<String> getAddExports() {
        return addExports;
    }

    public List<String> getAddOpens() {
        return addOpens;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getMainModule() {
        return mainModule;
    }

    public Map<String, JAppClasspathItem> getModulePathItems() {
        return modulePath;
    }

    public Map<String, JAppClasspathItem> getClassPathItems() {
        return classPath;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    public JAppResource findResource(boolean isModulePath, String itemName, String path) {
        ensureResolved();

        JAppClasspathItem item = isModulePath ? modulePath.get(itemName) : classPath.get(itemName);
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
