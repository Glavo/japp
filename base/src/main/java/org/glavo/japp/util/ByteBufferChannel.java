package org.glavo.japp.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

public final class ByteBufferChannel implements SeekableByteChannel {
    private ByteBuffer buffer;

    public ByteBufferChannel(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    private void ensureOpen() throws IOException {
        if (buffer == null) {
            throw new ClosedChannelException();
        }
    }

    @Override
    public boolean isOpen() {
        return buffer != null;
    }

    @Override
    public long position() throws IOException {
        ensureOpen();
        return buffer.position();
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        return buffer.capacity();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ensureOpen();

        int remaining = buffer.remaining();

        if (remaining == 0) {
            return -1;
        }

        int n = Math.min(dst.remaining(), remaining);
        int end = buffer.position() + n;
        dst.put(buffer.duplicate().limit(end));
        buffer.position(end);
        return n;
    }

    @Override
    public void close() throws IOException {
        buffer = null;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (newPosition < 0 || newPosition >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Illegal position " + newPosition);
        }
        this.buffer.position(Math.min((int) newPosition, buffer.limit()));
        return this;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
    }
}
