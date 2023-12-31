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
package org.glavo.japp.maven;

import org.glavo.japp.JAppProperties;
import org.glavo.japp.util.XxHash64;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.concurrent.TimeoutException;

public abstract class MavenRepository {

    static String fileName(String artifact, String version, String classifier) {
        return classifier == null
                ? artifact + "-" + version + ".jar"
                : artifact + "-" + version + "-" + classifier + ".jar";
    }

    final String name;

    MavenRepository(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Path resolve(String group, String artifact, String version, String classifier) throws Exception;

    public static final class Local extends MavenRepository {
        private final Path dir;

        public Local(String name, Path dir) {
            super(name);
            this.dir = dir;
        }

        @Override
        public Path resolve(String group, String artifact, String version, String classifier) throws IOException {
            String fileName = fileName(artifact, version, classifier);
            Path file = dir.resolve(group).resolve(artifact).resolve(version).resolve(fileName);
            if (!Files.isRegularFile(file)) {
                throw new IOException(fileName + " not exists or is not a regular file");
            }
            return file;
        }
    }

    public static final class Remote extends MavenRepository {
        private final String baseUrl;

        public Remote(String name, String baseUrl) {
            super(name);
            this.baseUrl = baseUrl;
        }

        @SuppressWarnings("deprecation")
        private static byte[] downloadAndVerity(String url, Long checksum) throws IOException {
            byte[] data;

            URLConnection connection = new URL(url).openConnection();
            try (InputStream input = connection.getInputStream()) {
                data = input.readAllBytes();
            }

            if (checksum != null) {
                long actual = XxHash64.hash(data);
                if (actual != checksum) {
                    throw new IOException(String.format(
                            "Failed while verifying %s (expected=%x, actual=%x)",
                            url,
                            checksum, actual));
                }
            }

            return data;
        }

        @Override
        public Path resolve(String group, String artifact, String version, String classifier) throws Exception {
            String fileName = fileName(artifact, version, classifier);

            Path cacheDir = JAppProperties.getHomeDirectory()
                    .resolve("cache")
                    .resolve("maven")
                    .resolve(name)
                    .resolve(group)
                    .resolve(artifact)
                    .resolve(version);
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }

            Path file = cacheDir.resolve(fileName);
            if (Files.isRegularFile(file)) {
                return file;
            }

            byte[] data;
            Path lockFile = cacheDir.resolve(fileName + ".lock");
            try (FileChannel channel = FileChannel.open(lockFile, EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE))) {
                FileLock lock = channel.tryLock();
                if (lock == null) {
                    for (int i = 0; i < 600; i++) {
                        Thread.sleep(100);
                        lock = channel.tryLock();
                        if (lock != null) {
                            break;
                        }
                    }

                    if (lock != null) {
                        if (Files.isRegularFile(file)) {
                            return file;
                        }
                    } else {
                        throw new TimeoutException();
                    }
                }

                String url = baseUrl + "/" + group.replace('.', '/') + "/" + artifact + "/" + version + "/" + fileName;
                data = downloadAndVerity(url, null);
                Path temp = cacheDir.resolve(fileName + ".temp");
                Files.write(temp, data);
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.deleteIfExists(lockFile);

            return file;
        }
    }
}
