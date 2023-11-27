package org.glavo.japp.boot;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
import org.glavo.japp.json.JSONArray;
import org.glavo.japp.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public final class JAppResourceGroup extends LinkedHashMap<String, JAppResource> {

    private static final byte MAGIC_NUMBER = (byte) 0xeb;

    private String name;

    public JAppResourceGroup() {
    }

    public JAppResourceGroup(String name) {
        this.name = name;
    }

    public void initName(String name) {
        if (this.name != null) {
            throw new IllegalStateException();
        }

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void readResources(JSONArray jsonArray) {
        if (jsonArray == null) {
            return;
        }

        for (Object o : jsonArray) {
            JAppResource resource = JAppResource.fromJson((JSONObject) o);
            put(resource.getName(), resource);
        }
    }

    public void readFrom(ByteBuffer buffer) throws IOException {
        byte magic = buffer.get();
        if (magic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong resource magic: %02x", magic));
        }

        int compressMethodId = Byte.toUnsignedInt(buffer.get());
        CompressionMethod compressionMethod = CompressionMethod.of(compressMethodId);
        if (compressionMethod == null) {
            throw new IOException(String.format("Unknown compression method: %02x", compressMethodId));
        }

        short reserved = buffer.getShort();
        if (reserved != 0) {
            throw new IOException("Reserved is not 0");
        }

        long uncompressedSize = Integer.toUnsignedLong(buffer.getInt());
        long compressedSize = Integer.toUnsignedLong(buffer.getInt());
        long resourcesCount = Integer.toUnsignedLong(buffer.getInt());

        if (buffer.remaining() < compressedSize) {
            throw new IOException("Compressed size is incorrect");
        }

        if (resourcesCount == 0) {
            if (uncompressedSize != 0) {
                throw new IOException();
            }
            if (compressedSize != 0) {
                buffer.position(Math.toIntExact(Math.addExact(buffer.position(), compressedSize)));
            }
            return;
        }

        ByteBuffer uncompressedBuffer;
        switch (compressionMethod) {
            case NONE: {
                uncompressedBuffer = buffer.duplicate().limit(Math.toIntExact(Math.addExact(buffer.position(), compressedSize)))
                        .order(ByteOrder.LITTLE_ENDIAN);
                buffer.position(uncompressedBuffer.limit());
                break;
            }
            case ZSTD: {
                uncompressedBuffer = ByteBuffer.allocate(Math.toIntExact(compressedSize)).order(ByteOrder.LITTLE_ENDIAN);
                ZstdUtils.decompress(buffer, uncompressedBuffer);
                if (uncompressedBuffer.hasRemaining()) {
                    throw new IOException();
                }
                uncompressedBuffer.flip();
                break;
            }
            default:
                throw new IOException("Unsupported compress method: " + compressionMethod);
        }

        for (int i = 0; i < resourcesCount; i++) {
            JAppResource resource = JAppResource.readFrom(uncompressedBuffer);
            this.put(resource.getName(), resource);
        }

        if (uncompressedBuffer.hasRemaining()) {
            throw new IOException();
        }
    }

    public static JAppResourceGroup fromJson(JSONArray array) {
        JAppResourceGroup group = new JAppResourceGroup();
        group.readResources(array);
        return group;
    }

    public JSONArray toJson() {
        JSONArray jsonArray = new JSONArray(this.size());
        for (JAppResource resource : this.values()) {
            jsonArray.put(resource.toJson());
        }
        return jsonArray;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }

}
