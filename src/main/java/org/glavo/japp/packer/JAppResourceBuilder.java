package org.glavo.japp.packer;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.boot.JAppResourceField;
import org.glavo.japp.util.ByteBufferOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;

public final class JAppResourceBuilder {
    private final String name;

    private final long offset;
    private final long size;

    private final CompressionMethod method;
    private final long compressedSize;

    private FileTime creationTime;
    private FileTime lastModifiedTime;
    private FileTime lastAccessTime;

    private Long checksum;

    public JAppResourceBuilder(String name, long offset, long size, CompressionMethod method, long compressedSize) {
        this.name = name;
        this.offset = offset;
        this.size = size;
        this.method = method;
        this.compressedSize = compressedSize;
    }

    public JAppResourceBuilder(String name, long offset, long size, CompressionMethod method, long compressedSize, FileTime creationTime, FileTime lastModifiedTime, FileTime lastAccessTime, Long checksum) {
        this.name = name;
        this.offset = offset;
        this.size = size;
        this.method = method;
        this.compressedSize = compressedSize;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
        this.lastAccessTime = lastAccessTime;
        this.checksum = checksum;
    }

    public void setCreationTime(FileTime creationTime) {
        this.creationTime = creationTime;
    }

    public void setLastModifiedTime(FileTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public void setLastAccessTime(FileTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public void setChecksum(Long checksum) {
        this.checksum = checksum;
    }

    private static void writeFileTime(ByteBufferOutputStream output, JAppResourceField field, FileTime time) {
        if (time != null) {
            output.writeByte(field.id());
            output.writeLong(time.toMillis());
        }
    }

    public void writeTo(ByteBufferOutputStream output) throws IOException {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

        output.writeByte(JAppResource.MAGIC_NUMBER);
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

    @Override
    public String toString() {
        return String.format("JAppResourceBuilder{name=%s, offset=%d, size=%d, method=%s, compressedSize=%d, creationTime=%s, lastModifiedTime=%s, lastAccessTime=%s, checksum=%d}",
                name, offset, size, method, compressedSize, creationTime, lastModifiedTime, lastAccessTime, checksum);
    }
}
