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

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class JAppProperties {

    private static final Path PROJECT_DIRECTORY;
    private static final Path HOME_DIRECTORY;
    private static final Path BOOT_JAR;

    static {
        Properties properties = new Properties();

        //noinspection DataFlowIssue
        try (Reader reader = new InputStreamReader(JAppProperties.class.getResourceAsStream("japp.properties"), UTF_8)) {
            properties.load(reader);
        } catch (Exception e) {
            throw new AssertionError(e);
        }

        // In the early stages we isolate the configuration in the project directory
        PROJECT_DIRECTORY = Paths.get(properties.getProperty("Project-Directory"));
        HOME_DIRECTORY = PROJECT_DIRECTORY.resolve(".japp");
        BOOT_JAR = Paths.get(properties.getProperty("Boot-Jar"));
    }

    public static Path getProjectDirectory() {
        return PROJECT_DIRECTORY;
    }

    public static Path getHomeDirectory() {
        return HOME_DIRECTORY;
    }

    public static Path getBootJar() {
        return BOOT_JAR;
    }

    private JAppProperties() {
    }
}
