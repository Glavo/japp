package org.glavo.japp.util;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public final class IOUtils {

    public static void readFully(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {

        //noinspection StatementWithEmptyBody
        while (channel.read(buffer) > 0) {
        }

        if (buffer.hasRemaining()) {
            throw new EOFException("Unexpected end of data");
        }
    }

    private IOUtils() {
    }
}
