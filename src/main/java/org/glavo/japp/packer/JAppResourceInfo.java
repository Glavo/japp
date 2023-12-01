package org.glavo.japp.packer;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.boot.JAppResourceField;
import org.glavo.japp.util.ByteBufferOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;

public final class JAppResourceInfo {
    final String name;

    FileTime creationTime;
    FileTime lastModifiedTime;
    FileTime lastAccessTime;

    boolean hasWritten = false;
    long offset;
    long size;
    CompressionMethod method;
    long compressedSize;

    Long checksum;

    public JAppResourceInfo(String name) {
        this.name = name;
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

    private static void writeFileTime(ByteBufferOutputStream output, JAppResourceField field, FileTime time) {
        if (time != null) {
            output.writeByte(field.id());
            output.writeLong(time.toMillis());
        }
    }

    void writeTo(ByteBufferOutputStream output) throws IOException {
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
}
