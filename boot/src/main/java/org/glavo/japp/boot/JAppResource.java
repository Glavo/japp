package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.TODO;
import org.glavo.japp.json.JSONObject;
import org.glavo.japp.util.ByteBufferBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;

public final class JAppResource {

    private static final byte MAGIC_NUMBER = (byte) 0xaa;
    private static final long NO_TIME = Long.MIN_VALUE;

    private final String name;

    private final long offset;
    private final long size;

    private final CompressionMethod method;
    private final long compressedSize;

    private long creationTime = NO_TIME;
    private long lastModifiedTime = NO_TIME;
    private long lastAccessTime = NO_TIME;

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
                        long creationTime, long lastModifiedTime, long lastAccessTime) {
        this.name = name;
        this.offset = offset;
        this.size = size;
        this.lastAccessTime = lastAccessTime;
        this.lastModifiedTime = lastModifiedTime;
        this.creationTime = creationTime;
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

    public FileTime getCreationTime() {
        return creationTime != NO_TIME ? FileTime.fromMillis(creationTime) : FileTime.fromMillis(0L);
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime != NO_TIME ? FileTime.fromMillis(lastModifiedTime) : getCreationTime();
    }

    public FileTime getLastAccessTime() {
        return lastAccessTime != NO_TIME ? FileTime.fromMillis(lastAccessTime) : getLastModifiedTime();
    }

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

        int compressMethodId = Byte.toUnsignedInt(buffer.get());
        CompressionMethod compressionMethod = CompressionMethod.of(compressMethodId);
        if (compressionMethod == null) {
            throw new IOException(String.format("Unknown compression method: %02x", compressMethodId));
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

        int fieldId;
        while ((fieldId = Byte.toUnsignedInt(buffer.get())) != 0) {
            JAppResourceField field = JAppResourceField.of(fieldId);
            if (field == null) {
                throw new IOException(String.format("Unknown field: %02x", fieldId));
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
                    if (time == NO_TIME) {
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

                    if (oldTime != NO_TIME) {
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

    private static void writeFileTime(ByteBufferBuilder builder, JAppResourceField field, long time) {
        if (time != NO_TIME) {
            builder.putByte(field.id());
            builder.putLong(time);
        }
    }

    public static void writeTo(ByteBufferBuilder builder,
                               String name, long offset, long size,
                               CompressionMethod method, long compressedSize,
                               long creationTime, long lastModifiedTime, long lastAccessTime,
                               Long checksum) throws IOException {
        builder.putByte(MAGIC_NUMBER);
        builder.putByte(method.id());
        builder.putShort((short) 0); // TODO
        builder.putLong(offset);
        builder.putUnsignedInt(size);
        builder.putUnsignedInt(compressedSize);

        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        builder.putUnsignedShort(bytes.length);
        builder.putBytes(bytes);

        if (checksum != null) {
            builder.putByte(JAppResourceField.CHECKSUM.id());
            builder.putLong(checksum);
        }

        writeFileTime(builder, JAppResourceField.FILE_CREATE_TIME, creationTime);
        writeFileTime(builder, JAppResourceField.FILE_LAST_MODIFIED_TIME, lastModifiedTime);
        writeFileTime(builder, JAppResourceField.FILE_LAST_ACCESS_TIME, lastAccessTime);

        builder.putByte(JAppResourceField.END.id());
    }

    public static JAppResource fromJson(JSONObject obj) {
        String name = obj.getString("Name");
        long offset = obj.getLong("Offset");
        long size = obj.getLong("Size");
        long creationTime = obj.optLong("Creation-Time", NO_TIME);
        long lastModifiedTime = obj.optLong("Last-Modified-Time", NO_TIME);
        long lastAccessTime = obj.optLong("Last-Access-Time", NO_TIME);
        String method = obj.optString("Compression-Method", null);
        long compressedSize = method == null ? size : obj.getLong("Compressed-Size");

        return new JAppResource(
                name,
                offset, size,
                CompressionMethod.valueOf(method), compressedSize,
                creationTime, lastModifiedTime, lastAccessTime
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
