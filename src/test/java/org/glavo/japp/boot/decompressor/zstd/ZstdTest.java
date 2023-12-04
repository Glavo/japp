/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.boot.decompressor.zstd;

import com.github.luben.zstd.Zstd;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

public class ZstdTest {

    private static void testDecompress(byte[] bytes, boolean checksum) throws Throwable {
        byte[] compressed;
        if (checksum) {
            byte[] tmp = new byte[bytes.length * 2 + 128];
            long len = Zstd.compress(tmp, bytes, Zstd.defaultCompressionLevel(), true);
            compressed = Arrays.copyOf(tmp, Math.toIntExact(len));
        } else {
            compressed = Zstd.compress(bytes);
        }

        byte[] decompressed = new byte[bytes.length];
        int decompressedLen = ZstdUtils.decompress(compressed, 0, compressed.length, decompressed, 0, decompressed.length);
        assertArrayEquals(bytes, decompressed);
        assertEquals(bytes.length, decompressedLen);

        Arrays.fill(decompressed, (byte) 0);
        ByteBuffer compressedBuffer = ByteBuffer.wrap(compressed);
        ByteBuffer decompressedBuffer = ByteBuffer.wrap(decompressed);

        decompressedLen = ZstdUtils.decompress(compressedBuffer, decompressedBuffer);
        assertFalse(compressedBuffer.hasRemaining());
        assertFalse(decompressedBuffer.hasRemaining());
        assertArrayEquals(bytes, decompressed);
        assertEquals(bytes.length, decompressedLen);

        compressedBuffer = ByteBuffer.allocateDirect(compressed.length);
        compressedBuffer.put(compressed);
        compressedBuffer.flip();
        decompressedBuffer = ByteBuffer.allocateDirect(bytes.length);

        decompressedLen = ZstdUtils.decompress(compressedBuffer, decompressedBuffer);
        assertFalse(compressedBuffer.hasRemaining());
        assertFalse(decompressedBuffer.hasRemaining());

        decompressedBuffer.flip();
        decompressedBuffer.get(decompressed);

        assertArrayEquals(bytes, decompressed);
        assertEquals(bytes.length, decompressedLen);
    }

    @Test
    void testDecompressor() throws Throwable {
        for (int len = 0; len <= 128; len++) {
            for (int seed = 0; seed < 100; seed++) {
                byte[] bytes = new byte[len];
                new Random(seed).nextBytes(bytes);
                try {
                    testDecompress(bytes, false);
                    testDecompress(bytes, true);
                } catch (Throwable e) {
                    throw new AssertionError(String.format("seed=%s, len=%s", seed, len), e);
                }
            }
        }

        try (ZipFile zf = new ZipFile(Paths.get(Zstd.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile())) {
            for (ZipEntry entry : Collections.list(zf.entries())) {
                if (!entry.isDirectory()) {
                    byte[] bytes = zf.getInputStream(entry).readAllBytes();
                    try {
                        testDecompress(bytes, false);
                        testDecompress(bytes, true);
                    } catch (Throwable e) {
                        throw new AssertionError("entry=" + entry.getName());
                    }
                }
            }
        }
    }
}
