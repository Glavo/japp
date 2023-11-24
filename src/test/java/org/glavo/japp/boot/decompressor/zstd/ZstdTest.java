package org.glavo.japp.boot.decompressor.zstd;

import com.github.luben.zstd.Zstd;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        Assertions.assertArrayEquals(bytes, decompressed);
        Assertions.assertEquals(bytes.length, decompressedLen);
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
