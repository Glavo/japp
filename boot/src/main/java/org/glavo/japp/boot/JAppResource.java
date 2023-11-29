package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.TODO;
import org.glavo.japp.util.ByteBufferOutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;

public final class JAppResource {

    private static final byte MAGIC_NUMBER = (byte) 0x1b;
    public static final long NO_TIME = Long.MIN_VALUE;

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
        if (flags != 0) {
            throw new IOException("Unsupported flags: " + Integer.toBinaryString(Short.toUnsignedInt(flags)));
        }

        long uncompressedSize = Integer.toUnsignedLong(buffer.getInt());
        long compressedSize = Integer.toUnsignedLong(buffer.getInt());
        long offset = buffer.getLong();

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

    private static void writeFileTime(ByteBufferOutputStream output, JAppResourceField field, long time) {
        if (time != NO_TIME) {
            output.writeByte(field.id());
            output.writeLong(time);
        }
    }

    public void writeTo(ByteBufferOutputStream output) throws IOException {
        writeTo(output, name, offset, size,
                method, compressedSize,
                creationTime, lastModifiedTime, lastAccessTime,
                needCheck ? checksum : null
        );
    }

    public static void writeTo(ByteBufferOutputStream output,
                               String name, long offset, long size,
                               CompressionMethod method, long compressedSize,
                               long creationTime, long lastModifiedTime, long lastAccessTime,
                               Long checksum) throws IOException {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

        output.writeByte(MAGIC_NUMBER);
        output.writeByte(method.id());
        output.writeShort((short) 0); // TODO
        output.writeUnsignedInt(size);
        output.writeUnsignedInt(compressedSize);
        output.writeLong(offset);
        output.writeUnsignedShort(nameBytes.length);
        output.writeBytes(nameBytes);

        if (checksum != null) {
            output.writeByte(JAppResourceField.CHECKSUM.id());
            output.writeLong(checksum);
        }

        writeFileTime(output, JAppResourceField.FILE_CREATE_TIME, creationTime);
        writeFileTime(output, JAppResourceField.FILE_LAST_MODIFIED_TIME, lastModifiedTime);
        writeFileTime(output, JAppResourceField.FILE_LAST_ACCESS_TIME, lastAccessTime);

        output.writeByte(JAppResourceField.END.id());
    }

//    @Override
//    public String toString() {
//        return getClass().getSimpleName() + toJson();
//    }
}
