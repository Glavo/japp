package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.TODO;
import org.glavo.japp.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;

public final class JAppResource {

    private static final byte MAGIC_NUMBER = (byte) 0xaa;

    private final String name;

    private final long offset;
    private final long size;

    private final CompressionMethod method;
    private final long compressedSize;

    private long creationTime = -1L;
    private long lastModifiedTime = -1L;
    private long lastAccessTime = -1L;

    private boolean needCheck;
    private long checksum;

    public JAppResource(String name, long offset, long size, CompressionMethod method, long compressedSize) {
        this.name = name;
        this.offset = offset;
        this.size = size;
        this.method = method;
        this.compressedSize = compressedSize;
    }

    public JAppResource(String name, long offset, long size,
                        CompressionMethod method, long compressedSize,
                        FileTime lastAccessTime, FileTime lastModifiedTime, FileTime creationTime) {
        this.name = name;
        this.offset = offset;
        this.size = size;
        this.lastAccessTime = lastAccessTime != null ? lastAccessTime.toMillis() : -1L;
        this.lastModifiedTime = lastModifiedTime != null ? lastModifiedTime.toMillis() : -1L;
        this.creationTime = creationTime != null ? creationTime.toMillis() : -1L;
        this.method = method;
        this.compressedSize = compressedSize;
    }

    public String getName() {
        return name;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }

//    public FileTime getLastAccessTime() {
//        return lastAccessTime;
//    }
//
//    public FileTime getLastModifiedTime() {
//        return lastModifiedTime;
//    }
//
//    public FileTime getCreationTime() {
//        return creationTime;
//    }

    public CompressionMethod getMethod() {
        return method;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public static JAppResource readFrom(ByteBuffer buffer) throws IOException {
        byte magic = buffer.get();
        if (magic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong resource magic: %02x", magic));
        }

        int compressMethodIndex = Byte.toUnsignedInt(buffer.get());
        CompressionMethod compressionMethod = CompressionMethod.of(compressMethodIndex);
        if (compressionMethod == null) {
            throw new IOException(String.format("Unknown compression method: %02x", compressMethodIndex));
        }

        short flags = buffer.getShort();

        long offset = buffer.getLong();
        long uncompressedSize = Integer.toUnsignedLong(buffer.getInt());
        long compressedSize = Integer.toUnsignedLong(buffer.getInt());

        int pathLength = Short.toUnsignedInt(buffer.getShort());
        byte[] pathBuffer = new byte[pathLength];
        buffer.get(pathBuffer);
        String path = new String(pathBuffer, StandardCharsets.UTF_8);

        JAppResource resource = new JAppResource(path, offset, uncompressedSize, compressionMethod, compressedSize);

        int fieldIndex;
        while ((fieldIndex = Byte.toUnsignedInt(buffer.get())) != 0) {
            JAppResourceField field = JAppResourceField.of(fieldIndex);
            if (field == null) {
                throw new IOException(String.format("Unknown field: %02x", fieldIndex));
            }

            switch (field) {
                case CHECKSUM: {
                    long checksum = buffer.getLong();

                    if (resource.needCheck) {
                        throw new IOException("Duplicate field: " + field);
                    }

                    resource.needCheck = true;
                    resource.checksum = checksum;
                    break;
                }
                case FILE_CREATE_TIME:
                case FILE_LAST_MODIFIED_TIME:
                case FILE_LAST_ACCESS_TIME: {
                    long time = buffer.getLong();
                    if (time < 0) {
                        throw new IOException("Invalid time: " + time);
                    }

                    long oldTime;
                    if (field == JAppResourceField.FILE_CREATE_TIME) {
                        oldTime = resource.creationTime;
                    } else if (field == JAppResourceField.FILE_LAST_MODIFIED_TIME) {
                        oldTime = resource.lastModifiedTime;
                    } else {
                        oldTime = resource.lastAccessTime;
                    }

                    if (oldTime >= 0) {
                        throw new IOException("Duplicate field: " + field);
                    }

                    if (field == JAppResourceField.FILE_CREATE_TIME) {
                        resource.creationTime = time;
                    } else if (field == JAppResourceField.FILE_LAST_MODIFIED_TIME) {
                        resource.lastModifiedTime = time;
                    } else {
                        resource.lastAccessTime = time;
                    }
                    break;
                }
                default:
                    throw new TODO("Field: " + field);
            }
        }

        return resource;
    }

    public static JAppResource fromJson(JSONObject obj) {
        String name = obj.getString("Name");
        long offset = obj.getLong("Offset");
        long size = obj.getLong("Size");
        long lastAccessTime = obj.optLong("Last-Access-Time", -1L);
        long lastModifiedTime = obj.optLong("Last-Modified-Time", -1L);
        long creationTime = obj.optLong("Creation-Time", -1L);
        String method = obj.optString("Compression-Method", null);
        long compressedSize = method == null ? size : obj.getLong("Compressed-Size");

        return new JAppResource(
                name,
                offset, size,
                CompressionMethod.valueOf(method), compressedSize, lastAccessTime > 0 ? FileTime.fromMillis(lastAccessTime) : null,
                lastModifiedTime > 0 ? FileTime.fromMillis(lastModifiedTime) : null,
                creationTime > 0 ? FileTime.fromMillis(creationTime) : null
        );
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        obj.putOpt("Name", name);
        obj.putOnce("Offset", offset);
        obj.putOnce("Size", size);

        if (lastAccessTime > 0) {
            obj.putOnce("Last-Access-Time", lastAccessTime);
        }
        if (lastModifiedTime > 0) {
            obj.putOnce("Last-Modified-Time", lastModifiedTime);
        }
        if (creationTime > 0) {
            obj.putOnce("Creation-Time", creationTime);
        }

        if (method != null) {
            obj.putOnce("Compression-Method", method.toString());
            obj.putOnce("Compressed-Size", compressedSize);
        }

        return obj;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + toJson();
    }
}
