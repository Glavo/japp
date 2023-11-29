package org.glavo.japp.boot;

import com.github.luben.zstd.Zstd;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
import org.glavo.japp.json.JSONArray;
import org.glavo.japp.json.JSONObject;
import org.glavo.japp.util.ByteBufferOutputStream;
import org.glavo.japp.util.IOUtils;
import org.glavo.japp.util.XxHash64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public final class JAppBootMetadata {
    private static final int MAGIC_NUMBER = 0x544f4f42;

    public static JAppBootMetadata readFrom(SeekableByteChannel channel) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocate(JAppResourceGroup.HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        headerBuffer.limit(8);
        IOUtils.readFully(channel, headerBuffer);
        headerBuffer.flip();

        int bootMagic = headerBuffer.getInt();
        if (bootMagic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong boot magic: %02x", bootMagic));
        }

        int groupCount = headerBuffer.getInt();

        JAppResourceGroup[] groups = new JAppResourceGroup[groupCount];
        {
            headerBuffer.limit(JAppResourceGroup.HEADER_LENGTH);

            ByteBuffer compressedBuffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
            ByteBuffer uncompressedBuffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < groupCount; i++) {
                headerBuffer.position(0);
                IOUtils.readFully(channel, headerBuffer);
                headerBuffer.flip();

                byte magic = headerBuffer.get();
                if (magic != JAppResourceGroup.MAGIC_NUMBER) {
                    throw new IOException(String.format("Wrong resource magic: %02x", magic));
                }

                int compressMethodId = Byte.toUnsignedInt(headerBuffer.get());
                CompressionMethod compressionMethod = CompressionMethod.of(compressMethodId);
                if (compressionMethod == null) {
                    throw new IOException(String.format("Unknown compression method: %02x", compressMethodId));
                }

                short reserved = headerBuffer.getShort();
                if (reserved != 0) {
                    throw new IOException("Reserved is not 0");
                }

                int uncompressedSize = headerBuffer.getInt();
                int compressedSize = headerBuffer.getInt();
                int resourcesCount = headerBuffer.getInt();
                long checksum = headerBuffer.getLong();

                assert !headerBuffer.hasRemaining();

                JAppResourceGroup group = new JAppResourceGroup();

                if (compressedBuffer.capacity() >= compressedSize) {
                    compressedBuffer.clear();
                } else {
                    int nextLen = Math.max(compressedSize, compressedBuffer.capacity() * 2);
                    compressedBuffer = ByteBuffer.allocate(nextLen).order(ByteOrder.LITTLE_ENDIAN);
                }
                compressedBuffer.limit(compressedSize);
                IOUtils.readFully(channel, compressedBuffer);
                compressedBuffer.flip();

                ByteBuffer uncompressed;
                if (compressionMethod == CompressionMethod.NONE) {
                    uncompressed = compressedBuffer;
                } else {
                    if (uncompressedBuffer.capacity() >= uncompressedSize) {
                        uncompressedBuffer.clear();
                    } else {
                        int nextLen = Math.max(uncompressedSize, uncompressedBuffer.capacity() * 2);
                        uncompressedBuffer = ByteBuffer.allocate(nextLen).order(ByteOrder.LITTLE_ENDIAN);
                    }
                    uncompressedBuffer.limit(uncompressedSize);

                    if (compressionMethod == CompressionMethod.ZSTD) {
                        ZstdUtils.decompress(compressedBuffer, uncompressedBuffer);
                        if (uncompressedBuffer.hasRemaining()) {
                            throw new IOException();
                        }
                        uncompressedBuffer.flip();
                    } else {
                        throw new IOException("Unsupported compress method: " + compressionMethod);
                    }
                    uncompressed = uncompressedBuffer;
                }

                long actualChecksum = XxHash64.hashByteBufferWithoutUpdate(uncompressed);
                if (actualChecksum != checksum) {
                    throw new IOException(String.format(
                            "Failed while verifying resource group at index %d (expected=%x, actual=%x)",
                            i, checksum, actualChecksum
                    ));
                }

                for (int j = 0; j < resourcesCount; j++) {
                    JAppResource resource = JAppResource.readFrom(uncompressed);
                    group.put(resource.getName(), resource);
                }

                if (uncompressedBuffer.hasRemaining()) {
                    throw new IOException();
                }

                groups[i] = group;
            }
        }

        return new JAppBootMetadata(Arrays.asList(groups), null); // TODO: Pool
    }

    public void writeTo(ByteBufferOutputStream output) throws IOException {
        output.writeInt(MAGIC_NUMBER);
        output.writeInt(groups.size());
        for (JAppResourceGroup group : groups) {
            ByteBufferOutputStream groupBodyBuilder = new ByteBufferOutputStream();
            for (JAppResource resource : group.values()) {
                resource.writeTo(groupBodyBuilder);
            }
            byte[] groupBody = groupBodyBuilder.toByteArray();
            byte[] compressed = Zstd.compress(groupBody);
            CompressionMethod method;
            if (compressed.length < groupBody.length) {
                method = CompressionMethod.ZSTD;
            } else {
                method = CompressionMethod.NONE;
                compressed = groupBody;
            }

            long checksum = XxHash64.hash(groupBody);

            output.writeByte(JAppResourceGroup.MAGIC_NUMBER);
            output.writeByte(method.id());
            output.writeShort((short) 0); // reserved
            output.writeInt(groupBody.length);
            output.writeInt(compressed.length);
            output.writeInt(group.size());
            output.writeLong(checksum);
            output.writeBytes(compressed);
        }
    }

    public static JAppBootMetadata fromJson(JSONObject obj) throws IOException {
        JSONArray array = obj.getJSONArray("Groups");
        JAppResourceGroup[] groups = new JAppResourceGroup[array.length()];
        for (int i = 0; i < groups.length; i++) {
            groups[i] = JAppResourceGroup.fromJson(array.getJSONArray(i));
        }

        ByteArrayPool pool = ByteArrayPool.readPool(Channels.newChannel(new ByteArrayInputStream(
                Base64.getDecoder().decode(obj.getString("Pool"))
        )));
        return new JAppBootMetadata(Arrays.asList(groups), pool);
    }

    private final List<JAppResourceGroup> groups;
    private final ByteArrayPool pool;

    public JAppBootMetadata(List<JAppResourceGroup> groups, ByteArrayPool pool) {
        this.groups = groups;
        this.pool = pool;
    }

    public List<JAppResourceGroup> getGroups() {
        return groups;
    }

    public ByteArrayPool getPool() {
        return pool;
    }
}
