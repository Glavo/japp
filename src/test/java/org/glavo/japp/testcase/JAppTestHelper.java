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
package org.glavo.japp.testcase;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JAppTestHelper {
    private static final String jar = System.getProperty("japp.jar");

    private static String runJApp(String mode, List<String> args) throws IOException {
        ArrayList<String> list = new ArrayList<>();
        list.add(System.getProperty("java.home") + (System.getProperty("os.name").startsWith("Win") ? "\\bin\\java.exe" : "/bin/java"));
        list.add("-Dsun.stdout.encoding=UTF-8");
        list.add("-Dsun.stderr.encoding=UTF-8");
        list.add("-Dstdout.encoding=UTF-8");
        list.add("-Dstderr.encoding=UTF-8");
        list.add("-jar");
        list.add(jar);
        list.add(mode);
        list.addAll(args);

        try {
            Process process = Runtime.getRuntime().exec(list.toArray(new String[0]));
            int res = process.waitFor();
            if (res != 0) {
                throw new RuntimeException("Process exit code is " + res + ", stderr=" +
                                           new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
            }
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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

        ArrayList<String> argsList = new ArrayList<>();
        argsList.add("-o");
        argsList.add(targetFile.toString());
        Collections.addAll(argsList, args);
        runJApp("create", argsList);

        return new FileHolder(targetFile);
    }

    public static String launch(Path file, String... args) throws IOException {
        ArrayList<String> argsList = new ArrayList<>();
        argsList.add(file.toAbsolutePath().normalize().toString());
        Collections.addAll(argsList, args);
        return runJApp("run", argsList);
    }
}
