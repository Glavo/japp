package org.glavo.japp.boot.decompressor.classfile;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.boot.decompressor.zstd.ZstdUtils;
import org.glavo.japp.util.IOUtils;
import org.glavo.japp.util.MUTF8;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

public final class ByteArrayPool {
    public static final byte MAGIC_NUMBER = (byte) 0xf0;

    private final ByteBuffer bytes;
    private final long[] offsetAndSize;

    private ByteArrayPool(ByteBuffer bytes, long[] offsetAndSize) {
        this.bytes = bytes;
        this.offsetAndSize = offsetAndSize;
    }

    public static ByteArrayPool readFrom(ReadableByteChannel channel) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.readFully(channel, headerBuffer);
        headerBuffer.flip();

        byte magic = headerBuffer.get();
        if (magic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong boot magic: %02x", Byte.toUnsignedInt(magic)));
        }

        CompressionMethod compressionMethod = CompressionMethod.readFrom(headerBuffer);

        short reserved = headerBuffer.getShort();
        if (reserved != 0) {
            throw new IOException("Reserved is not zero");
        }

        int count = headerBuffer.getInt();
        int uncompressedBytesSize = headerBuffer.getInt();
        int compressedBytesSize = headerBuffer.getInt();

        long[] offsetAndSize = new long[count];

        int offset = 0;
        ByteBuffer sizes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.readFully(channel, sizes);
        sizes.flip();
        for (int i = 0; i < count; i++) {
            int s = Short.toUnsignedInt(sizes.getShort());
            offsetAndSize[i] = (((long) s) << 32) | (long) offset;
            offset += s;
        }

        byte[] compressedBytes = new byte[compressedBytesSize];
        IOUtils.readFully(channel, ByteBuffer.wrap(compressedBytes));

        byte[] uncompressedBytes;
        if (compressionMethod == CompressionMethod.NONE) {
            uncompressedBytes = compressedBytes;
        } else {
            uncompressedBytes = new byte[uncompressedBytesSize];
            if (compressionMethod == CompressionMethod.ZSTD) {
                ZstdUtils.decompress(compressedBytes, 0, compressedBytesSize, uncompressedBytes, 0, uncompressedBytesSize);
            } else {
                throw new IOException("Unsupported compression method: " + compressionMethod);
            }
        }

        return new ByteArrayPool(ByteBuffer.wrap(uncompressedBytes), offsetAndSize);
    }

    private void get(int offset, int size, byte[] out, int outOffset) {
        bytes.slice().limit(offset + size).position(offset).get(out, outOffset, size);
    }


    public ByteBuffer get(int index) {
        long l = offsetAndSize[index];
        int offset = (int) (l & 0xffff_ffffL);
        int size = (int) (l >>> 32);

        return bytes.slice().limit(offset + size).position(offset);
    }

    public int get(int index, byte[] out, int outOffset) {
        long l = offsetAndSize[index];
        int offset = (int) (l & 0xffff_ffffL);
        int size = (int) (l >>> 32);

        get(offset, size, out, outOffset);
        return size;
    }

    public int get(int index, ByteBuffer output) {
        int len = get(index, output.array(), output.arrayOffset() + output.position());
        output.position(output.position() + len);
        return len;
    }

    private static final byte[] CLASS_FILE_EXT = {'.', 'c', 'l', 'a', 's', 's'};

    public String getClassFileName(int packageNameIndex, int classNameIndex) {
        long pl = offsetAndSize[packageNameIndex];
        long cl = offsetAndSize[classNameIndex];

        int packageNameOffset = (int) (pl & 0xffff_ffffL);
        int packageNameSize = (int) (pl >>> 32);

        int classNameOffset = (int) (cl & 0xffff_ffffL);
        int classNameSize = (int) (cl >>> 32);

        byte[] mutf8;
        if (packageNameSize != 0) {
            mutf8 = new byte[packageNameSize + 1 + classNameSize + CLASS_FILE_EXT.length];
            get(packageNameOffset, packageNameSize, mutf8, 0);
            mutf8[packageNameSize] = '/';

            get(classNameOffset, classNameSize, mutf8, packageNameSize + 1);
            System.arraycopy(CLASS_FILE_EXT, 0, mutf8, packageNameSize + 1 + classNameSize, CLASS_FILE_EXT.length);
        } else {
            mutf8 = new byte[classNameSize + CLASS_FILE_EXT.length];
            get(classNameOffset, classNameSize, mutf8, 0);
            System.arraycopy(CLASS_FILE_EXT, 0, mutf8, classNameSize, CLASS_FILE_EXT.length);
        }
        return MUTF8.stringFromMUTF8(mutf8);
    }
}
