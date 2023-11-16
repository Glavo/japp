package org.glavo.japp.classfile;

import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.packer.compressor.classfile.ByteArrayPoolBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteArrayPoolTest {

    private static byte[] testString(int index) {
        return ("str" + index).getBytes(UTF_8);
    }

    @Test
    void test() throws IOException {
        ByteArrayPoolBuilder builder = new ByteArrayPoolBuilder();

        for (int i = 0; i < 10; i++) {
            assertEquals(i, builder.add(testString(i)));
            assertEquals(i, builder.add(testString(i)));
        }

        ReadableByteChannel input = Channels.newChannel(new ByteArrayInputStream(builder.toByteArray()));
        ByteArrayPool pool = ByteArrayPool.readPool(input);

        for (int i = 0; i < 10; i++) {
            byte[] testString = testString(i);

            ByteBuffer buffer = ByteBuffer.allocate(20);
            assertEquals(testString.length, pool.get(i, buffer));
            assertEquals(testString.length, buffer.position());

            byte[] out = new byte[testString.length];
            buffer.flip().get(out);
            assertArrayEquals(testString, out);
        }
    }
}
