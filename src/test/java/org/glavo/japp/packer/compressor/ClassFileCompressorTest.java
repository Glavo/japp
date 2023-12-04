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
package org.glavo.japp.packer.compressor;

import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.boot.decompressor.classfile.ClassFileDecompressor;
import org.glavo.japp.packer.compressor.classfile.ByteArrayPoolBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassFileCompressorTest {

    private static DynamicTest createTest(String name, byte[] bytes) {
        return DynamicTest.dynamicTest("Compress " + name, () -> {
            ByteArrayPoolBuilder poolBuilder = new ByteArrayPoolBuilder();
            CompressContext context = () -> poolBuilder;
            CompressResult result = Compressors.CLASSFILE.compress(context, bytes);

            byte[] output = new byte[bytes.length];
            ClassFileDecompressor.decompress(
                    poolBuilder.toPool(),
                    result.getCompressed(),
                    output
            );
            Assertions.assertArrayEquals(bytes, output);
        });
    }

    @TestFactory
    public Collection<DynamicTest> test() throws Throwable {
        Path jar = Paths.get(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        Map<String, byte[]> entries = new LinkedHashMap<>();
        List<DynamicTest> tests = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry element = enumeration.nextElement();
                String name = element.getName();
                if (name.endsWith(".class")) {
                    byte[] bytes = zipFile.getInputStream(element).readAllBytes();

                    tests.add(createTest(name, bytes));
                    entries.put(name, bytes);
                }
            }
        }

        tests.add(DynamicTest.dynamicTest("Compress ALL", () -> {
            ByteArrayPoolBuilder poolBuilder = new ByteArrayPoolBuilder();
            CompressContext context = () -> poolBuilder;

            Map<String, ByteBuffer> allCompressed = new HashMap<>();

            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                CompressResult result = Compressors.CLASSFILE.compress(context, entry.getValue());
                allCompressed.put(entry.getKey(), result.getCompressed());
            }


            ByteArrayPool pool = poolBuilder.toPool();

            Assertions.assertAll(entries.keySet().stream().map(key -> () -> {
                byte[] expected = entries.get(key);
                byte[] output = new byte[expected.length];
                ClassFileDecompressor.decompress(
                        pool,
                        allCompressed.get(key),
                        output
                );

                Assertions.assertArrayEquals(expected, output);
            }));
        }));

        return tests;
    }
}
