package org.glavo.japp.packer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class Packer implements Closeable {

    private static final short MAJOR_VERSION = -1;
    private static final short MINOR_VERSION = 0;

    private final byte[] ba = new byte[8];
    private final ByteBuffer bb = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);

    private final OutputStream outputStream;
    private long totalBytes = 0L;

    public Packer(OutputStream outputStream) {
        this.outputStream = Objects.requireNonNull(outputStream);
    }

    private void writeByte(byte b) throws IOException {
        outputStream.write(b & 0xff);
        totalBytes += 1;
    }

    private void writeShort(short s) throws IOException {
        bb.putShort(0, s);
        outputStream.write(ba, 0, 2);
        totalBytes += 2;
    }

    private void writeInt(int i) throws IOException {
        bb.putInt(0, i);
        outputStream.write(ba, 0, 4);
        totalBytes += 4;
    }

    private void writeLong(long l) throws IOException {
        bb.putLong(0, l);
        outputStream.write(ba, 0, 8);
        totalBytes += 8;
    }

    private void writeBytes(byte[] arr) throws IOException {
        writeBytes(arr, 0, arr.length);
    }

    private void writeBytes(byte[] arr, int offset, int len) throws IOException {
        outputStream.write(arr, offset, len);
        totalBytes += len;
    }

    private void writeFileEnd(long metadataOffset) throws IOException {
        long fileSize = totalBytes + 48;

        // magic number
        ba[0] = 'J';
        ba[1] = 'A';
        ba[2] = 'P';
        ba[3] = 'P';
        writeBytes(ba, 0, 4);

        // version number
        writeShort(MAJOR_VERSION);
        writeShort(MINOR_VERSION);

        // flags
        writeLong(0L);

        // file size
        writeLong(fileSize);

        // metadata offset
        writeLong(metadataOffset);

        // reserved
        writeLong(0L);
        writeLong(0L);

        if (totalBytes != fileSize) {
            throw new AssertionError();
        }
    }

    @Override
    public void close() throws IOException {
        // TODO
        writeFileEnd(0L);
    }

    public static void main(String[] args) {
        // TODO
    }
}
