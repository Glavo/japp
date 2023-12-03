package org.glavo.japp.util;

import java.nio.ByteBuffer;

public final class ByteBufferUtils {

    @SuppressWarnings("deprecation")
    private static final int release = Runtime.version().major();

    public static ByteBuffer slice(ByteBuffer buffer, int index, int length) {
        if (release >= 13) {
            //noinspection Since15
            return buffer.slice(index,length);
        } else {
            return buffer.duplicate().limit(index + length).position(index).slice();
        }
    }

    private ByteBufferUtils() {
    }
}
