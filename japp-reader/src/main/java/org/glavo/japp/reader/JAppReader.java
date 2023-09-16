package org.glavo.japp.reader;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

public final class JAppReader implements Closeable {
    public static final short MAJOR_VERSION = -1;
    public static final short MINOR_VERSION = 0;

    public static final int FILE_END_LENGTH = 48;

    private final ReentrantLock lock = new ReentrantLock();

    private final FileChannel channel;

    private final long contentOffset;

    public JAppReader(Path file) throws IOException {
        this.channel = FileChannel.open(file);

        try {
            long fileSize = channel.size();

            if (fileSize < FILE_END_LENGTH) {
                throw new IOException("File is too small");
            }

            int endBufferSize = (int) Math.min(fileSize, 8192);
            ByteBuffer endBuffer = ByteBuffer.allocate(endBufferSize).order(ByteOrder.LITTLE_ENDIAN);

            channel.position(fileSize - endBufferSize);

            while (channel.read(endBuffer) > 0) {
            }

            if (endBuffer.remaining() > 0) {
                throw new EOFException();
            }

            endBuffer.limit(endBufferSize).position(endBufferSize - FILE_END_LENGTH);

            int magicNumber = endBuffer.getInt();
            if (magicNumber != 0x5050414a) {
                throw new IOException("Invalid magic number: " + Long.toHexString(magicNumber));
            }

            short majorVersion = endBuffer.getShort();
            short minorVersion = endBuffer.getShort();

            if (majorVersion != MAJOR_VERSION || minorVersion != MINOR_VERSION) {
                throw new IOException("Version number mismatch");
            }

            long flags = endBuffer.getLong();

            long fileContentSize = endBuffer.getLong();
            long metadataOffset = endBuffer.getLong();

            assert endBuffer.remaining() == 16;

            if (flags != 0) {
                throw new IOException("Unsupported flag");
            }

            if (fileContentSize > fileSize || fileContentSize < FILE_END_LENGTH) {
                throw new IOException("Invalid file size: " + fileContentSize);
            }

            if (metadataOffset >= fileContentSize - FILE_END_LENGTH) {
                throw new IOException("Invalid metadata offset: " + metadataOffset);
            }

            this.contentOffset = fileSize - fileContentSize;

            // TODO

        } catch (Throwable e) {
            try {
                channel.close();
            } catch (Throwable e2) {
                e2.addSuppressed(e);
                throw e2;
            }

            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
