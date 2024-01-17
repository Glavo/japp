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
package org.glavo.japp.packer;

import com.github.luben.zstd.Zstd;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ModuleInfoReaderTest {
    @Test
    public void deriveAutomaticModuleNameTest() {
        assertEquals("a", ModuleInfoReader.deriveAutomaticModuleName("a.jar"));
        assertEquals("a", ModuleInfoReader.deriveAutomaticModuleName("a-0.1.0.jar"));
        assertEquals("a", ModuleInfoReader.deriveAutomaticModuleName("...a-0.1.0.jar"));
        assertEquals("a", ModuleInfoReader.deriveAutomaticModuleName("...a...-0.1.0.jar"));
        assertEquals("a.b", ModuleInfoReader.deriveAutomaticModuleName("a-b-0.1.0.jar"));
        assertEquals("a.b", ModuleInfoReader.deriveAutomaticModuleName("a--b-0.1.0.jar"));
    }

    static Stream<Arguments> readModuleNameTestArguments() {
        return Map.of(
                "org.junit.jupiter.api", Test.class,
                "com.github.luben.zstd_jni", Zstd.class
        ).entrySet().stream().map(entry -> {

            byte[] moduleInfo;
            try (ZipFile zipFile = new ZipFile(new File(entry.getValue().getProtectionDomain().getCodeSource().getLocation().toURI()))) {
                try (InputStream inputStream = zipFile.getInputStream(zipFile.getEntry("module-info.class"))) {
                    moduleInfo = inputStream.readAllBytes();
                }
            } catch (Exception e) {
                throw new AssertionError(e);
            }

            return Arguments.of(entry.getKey(), moduleInfo);
        });
    }

    @ParameterizedTest
    @MethodSource("readModuleNameTestArguments")
    public void readModuleNameTest(String moduleName, byte[] moduleInfo) throws IOException {
        assertEquals(moduleName, ModuleInfoReader.readModuleName(new ByteArrayInputStream(moduleInfo)));
    }
}
