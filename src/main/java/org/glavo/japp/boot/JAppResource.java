package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.thirdparty.json.JSONObject;

import java.nio.file.attribute.FileTime;

public class JAppResource {

    private final String name;

    private final long offset;
    private final long size;
    private final FileTime lastAccessTime;

    private final FileTime lastModifiedTime;
    private final FileTime creationTime;

    private final CompressionMethod method;
    private final long compressedSize;

    public JAppResource(String name, long offset, long size,
                        FileTime lastAccessTime, FileTime lastModifiedTime, FileTime creationTime,
                        CompressionMethod method, long compressedSize) {
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

    public FileTime getLastAccessTime() {
        return lastAccessTime;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public FileTime getCreationTime() {
        return creationTime;
    }

    public CompressionMethod getMethod() {
        return method;
    }

    public long getCompressedSize() {
        return compressedSize;
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
                lastAccessTime > 0 ? FileTime.fromMillis(lastAccessTime) : null,
                lastModifiedTime > 0 ? FileTime.fromMillis(lastModifiedTime) : null,
                creationTime > 0 ? FileTime.fromMillis(creationTime) : null,
                CompressionMethod.valueOf(method), compressedSize
        );
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        obj.putOpt("Name", name);
        obj.putOnce("Offset", offset);
        obj.putOnce("Size", size);

        if (lastAccessTime != null) {
            obj.putOnce("Last-Access-Time", lastAccessTime.toMillis());
        }
        if (lastModifiedTime != null) {
            obj.putOnce("Last-Modified-Time", lastModifiedTime.toMillis());
        }
        if (creationTime != null) {
            obj.putOnce("Creation-Time", creationTime.toMillis());
        }

        if (method != null) {
            obj.putOnce("Compression-Method", method.toString());
            obj.putOnce("Compressed-Size", compressedSize);
        }

        return obj;
    }

    @Override
    public String toString() {
        return "JAppResource" + toJson();
    }
}
