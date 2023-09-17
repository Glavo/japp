package org.glavo.japp.reader;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

public final class JAppReader implements Closeable {
    public static final short MAJOR_VERSION = -1;
    public static final short MINOR_VERSION = 0;

    public static final int FILE_END_SIZE = 48;

    private final ReentrantLock lock = new ReentrantLock();

    private final FileChannel channel;

    private final long contentOffset;

    public JAppReader(Path file) throws IOException {
        this.channel = FileChannel.open(file);

        try {
            long fileSize = channel.size();

            if (fileSize < FILE_END_SIZE) {
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

            endBuffer.limit(endBufferSize).position(endBufferSize - FILE_END_SIZE);

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

            if (fileContentSize > fileSize || fileContentSize < FILE_END_SIZE) {
                throw new IOException("Invalid file size: " + fileContentSize);
            }

            if (metadataOffset >= fileContentSize - FILE_END_SIZE) {
                throw new IOException("Invalid metadata offset: " + metadataOffset);
            }

            this.contentOffset = fileSize - fileContentSize;

            long metadataSize = fileContentSize - FILE_END_SIZE - metadataOffset;

            JSONObject json;
            if (metadataSize < endBufferSize - FILE_END_SIZE) {
                json = new JSONObject(new String(endBuffer.array(), (int) (endBufferSize - metadataSize - FILE_END_SIZE), (int) metadataSize, StandardCharsets.UTF_8));
            } else {
                if (metadataSize > (1 << 30)) {
                    throw new IOException("Metadata is too large");
                }

                ByteBuffer metadataBuffer = ByteBuffer.allocate((int) metadataSize);
                channel.position(contentOffset + metadataOffset);
                while (channel.read(metadataBuffer) > 0) {
                }

                if (metadataBuffer.remaining() > 0) {
                    throw new EOFException();
                }

                json = new JSONObject(new String(metadataBuffer.array(), StandardCharsets.UTF_8));
            }

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
