package org.glavo.japp.boot.decompressor.zstd;

import io.airlift.compress.zstd.ZstdCompressor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZstdTest {

    private static final ZstdCompressor compressor = new ZstdCompressor();

    private static void testDecompress(byte[] bytes) throws Throwable {
        byte[] compressed = new byte[compressor.maxCompressedLength(bytes.length)];
        int len = compressor.compress(bytes, 0, bytes.length, compressed, 0, compressed.length);

        byte[] decompressed = new byte[bytes.length];
        int decompressedLen = ZstdUtils.decompress(compressed, 0, len, decompressed, 0, decompressed.length);
        Assertions.assertArrayEquals(bytes, decompressed);
        Assertions.assertEquals(bytes.length, decompressedLen);
    }

    @Test
    void testDecompressor() throws Throwable {
        for (int len = 0; len < 100; len++) {
            for (int seed = 0; seed < 100; seed++) {
                byte[] bytes = new byte[len];
                new Random(0).nextBytes(bytes);
                try {
                    testDecompress(bytes);
                } catch (Throwable e) {
                    throw new AssertionError(String.format("seed=%s, len=%s", seed, len), e);
                }
            }
        }

        try (ZipFile zf = new ZipFile(Paths.get(ZstdCompressor.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile())) {
            for (ZipEntry entry : Collections.list(zf.entries())) {
                if (!entry.isDirectory()) {
                    try {
                        testDecompress(zf.getInputStream(entry).readAllBytes());
                    } catch (Throwable e) {
                        throw new AssertionError("entry=" + entry.getName());
                    }
                }
            }
        }
    }
}
