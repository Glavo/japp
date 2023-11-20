package org.glavo.japp.boot.jappfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;

final class ByteArrayChannel implements SeekableByteChannel {

    private byte[] array;
    private final int end;
    private int offset;

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
        return offset;
    }

    @Override
    public long size() throws IOException {
        return end;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        ensureOpen();

        if (offset == end) {
            return -1;
        }

        int n = Math.min(dst.remaining(), end - offset);
        dst.put(array, offset, n);
        offset += n;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
    }
}
