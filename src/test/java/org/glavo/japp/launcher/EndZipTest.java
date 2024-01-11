/*
 * Copyright (C) 2024 Glavo
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
package org.glavo.japp.launcher;

import com.github.luben.zstd.Zstd;
import org.glavo.japp.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EndZipTest {

    private static Stream<Path> jars() {
        return Stream.of(Test.class, Zstd.class).map(clazz -> {
            try {
                return Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
            } catch (URISyntaxException e) {
                throw new AssertionError(e);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("jars")
    void testGetSize(Path zipFile) throws IOException {
        try (FileChannel channel = FileChannel.open(zipFile)) {
            long fileSize = channel.size();

            int endBufferSize = (int) Math.max(fileSize, 8192);
            ByteBuffer endBuffer = ByteBuffer.allocate(endBufferSize).order(ByteOrder.LITTLE_ENDIAN);

            channel.position(fileSize - endBufferSize);
            IOUtils.readFully(channel, endBuffer);

            endBuffer.flip();

            assertEquals(fileSize, JAppLauncherMetadata.getEndZipSize(endBuffer));
        }
    }
}
