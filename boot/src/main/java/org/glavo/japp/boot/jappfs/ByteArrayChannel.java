package org.glavo.japp.boot.jappfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

final class ByteArrayChannel implements SeekableByteChannel {

    private byte[] array;
    private final int end;
    private int position;

    public ByteArrayChannel(byte[] array) {
        this.array = array;
        this.end = array.length;
    }

    private void ensureOpen() throws IOException {
        if (array == null) {
            throw new ClosedChannelException();
        }
    }

    @Override
    public boolean isOpen() {
        return array != null;
    }

    @Override
    public long position() throws IOException {
        ensureOpen();
        return position;
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        return end;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ensureOpen();

        if (position >= end) {
            return -1;
        }

        int n = Math.min(dst.remaining(), end - position);
        dst.put(array, position, n);
        position += n;
        return n;
    }

    @Override
    public void close() throws IOException {
        array = null;
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
        this.position = Math.min((int) newPosition, end);
        return this;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
    }
}
