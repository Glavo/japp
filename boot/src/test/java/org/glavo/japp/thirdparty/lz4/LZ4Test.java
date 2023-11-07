package org.glavo.japp.thirdparty.lz4;

import net.jpountz.lz4.LZ4Factory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LZ4Test {

    private static void testDecompress(byte[] bytes) throws Throwable {
        byte[] compressed = LZ4Factory.fastestInstance().highCompressor().compress(bytes);
        byte[] decompressed = LZ4Decompressor.decompress(compressed, bytes.length);
        Assertions.assertArrayEquals(bytes, decompressed);

        compressed = LZ4Factory.fastestInstance().fastCompressor().compress(bytes);
        decompressed = LZ4Decompressor.decompress(compressed, bytes.length);
        Assertions.assertArrayEquals(bytes, decompressed);
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

        try (ZipFile zf = new ZipFile(Paths.get(LZ4Factory.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile())) {
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
