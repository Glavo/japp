package org.glavo.japp.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.util.Objects;

public class ByteBufferInputStream extends InputStream {

    private final ByteBuffer buffer;

    public ByteBufferInputStream(byte[] array) {
        this.buffer = ByteBuffer.wrap(array);
    }

    public ByteBufferInputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int available() throws IOException {
        return buffer.remaining();
    }

    @Override
    public int read() throws IOException {
        if (buffer.hasRemaining()) {
            return Byte.toUnsignedInt(buffer.get());
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }

        int remaining = buffer.remaining();
        if (remaining == 0) {
            return -1;
        }

        int n = Math.min(remaining, len);
        buffer.get(b, off, n);
        return n;
    }

    // @Override
    public byte[] readAllBytes() throws IOException {
        int remaining = buffer.remaining();
        byte[] res = new byte[remaining];
        buffer.get(res);
        return res;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }

        int res = (int) Math.min(n, buffer.remaining());
        buffer.position(buffer.position() + res);
        return res;
    }

    // @Override
    @SuppressWarnings("Since15")
    public void skipNBytes(long n) throws IOException {
        if (n <= 0) {
            return;
        }

        int remaining = buffer.remaining();
        if (n > remaining) {
            throw new EOFException();
        }

        int res = (int) Math.min(n, buffer.remaining());
        buffer.position(buffer.position() + res);
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readlimit) {
        buffer.mark();
    }

    @Override
    public void reset() throws IOException {
        try {
            buffer.reset();
        } catch (InvalidMarkException e) {
            throw new IOException(e);
        }
    }
}
