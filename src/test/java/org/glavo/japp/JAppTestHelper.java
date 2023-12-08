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
package org.glavo.japp;

import org.glavo.japp.platform.JavaRuntime;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public final class JAppTestHelper {
    private static final Path jar = Paths.get(System.getProperty("org.glavo.japp.jar"));

    private static void runProcess(String[] commands) throws IOException {
        try {
            Process process = Runtime.getRuntime().exec(commands);
            int res = process.waitFor();
            if (res != 0) {
                throw new RuntimeException(new String(process.getErrorStream().readAllBytes()));
            }
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public static final class FileHolder implements Closeable {

        public final Path file;

        public FileHolder(Path file) {
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(file);
        }
    }

    public static FileHolder create(String... args) throws IOException {
        Path targetFile = Files.createTempFile("japp-test-", ".japp").toAbsolutePath();

        String execPath = JavaRuntime.fromDir(Paths.get(System.getProperty("java.home"))).getExec().toString();

        runProcess(Stream.concat(
                Stream.of(execPath, "-o", targetFile.toString()),
                Stream.of(args)
        ).toArray(String[]::new));

        return new FileHolder(targetFile);
    }
}
