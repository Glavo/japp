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

import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.Locale;

public class LWJGL {
    private static final String LWJGL_VERSION = "3.3.4";
    private static final String LWJGL_PLATFORM;

    static {
        String lwjglPlatform;

        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String archName = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if (osName.startsWith("win")) {
            lwjglPlatform = "windows";
        } else if (osName.contains("darwin") || osName.contains("mac") || osName.contains("osx")) {
            lwjglPlatform = "macos";
        } else {
            lwjglPlatform = "linux";
        }

        switch (archName) {
            case "x8664":
            case "x86-64":
            case "x86_64":
            case "amd64":
            case "ia32e":
            case "em64t":
            case "x64":
                break;
            case "x86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
                lwjglPlatform += "-x86";
                break;
            case "arm64":
            case "aarch64":
                lwjglPlatform += "-arm64";
                break;
            case "arm":
            case "arm32":
                lwjglPlatform += "-arm32";
                break;
            case "riscv64":
                lwjglPlatform += "-riscv64";
                break;
            case "ppc64le":
            case "powerpc64le":
                lwjglPlatform += "-ppc64le";
                break;
            default:
                if (archName.startsWith("armv7")) {
                    lwjglPlatform += "-arm32";
                } else if (archName.startsWith("armv8") || archName.startsWith("armv9")) {
                    lwjglPlatform += "-arm64";
                }
        }

        LWJGL_PLATFORM = lwjglPlatform;
    }

    public static void addDependency(DependencyHandler handler, String configurationName, String moduleName) {
        handler.add(configurationName, "org.lwjgl:" + moduleName + ":" + LWJGL_VERSION);
        handler.add(configurationName, "org.lwjgl:" + moduleName + ":" + LWJGL_VERSION + ":natives-" + LWJGL_PLATFORM);
    }
}
