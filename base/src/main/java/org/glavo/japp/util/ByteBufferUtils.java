package org.glavo.japp.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;

public final class ByteBufferUtils {

    private static final MethodHandle BYTE_BUFFER_SLICE;

    static {
        MethodHandle handle = null;
        try {
            handle = MethodHandles.publicLookup().findVirtual(
                    ByteBuffer.class, "slice",
                    MethodType.methodType(ByteBuffer.class, int.class, int.class)
            );
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        }

        BYTE_BUFFER_SLICE = handle;
    }

    public static ByteBuffer slice(ByteBuffer buffer, int index, int length) {
        if (BYTE_BUFFER_SLICE != null) {
            try {
                return (ByteBuffer) BYTE_BUFFER_SLICE.invokeExact(buffer, index, length);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            return buffer.duplicate().limit(index + length).position(index).slice();
        }
    }

    private ByteBufferUtils() {
    }
}
