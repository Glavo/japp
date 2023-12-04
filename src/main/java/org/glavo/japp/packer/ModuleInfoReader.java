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

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModuleInfoReader {
    private static final Pattern DASH_VERSION = Pattern.compile("-(\\d+(\\.|$))");
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern REPEATING_DOTS = Pattern.compile("(\\.)(\\1)+");
    private static final Pattern TRAILING_DOTS = Pattern.compile("\\.$");

    public static String deriveAutomaticModuleName(String jarFileName) {
        if (!jarFileName.endsWith(".jar")) {
            throw new IllegalArgumentException(jarFileName);
        }

        int end = jarFileName.length() - ".jar".length();

        int start;

        for (start = 0; start < end; start++) {
            if (jarFileName.charAt(start) != '.') {
                break;
            }
        }

        if (start == end) {
            throw new IllegalArgumentException(jarFileName);
        }

        String name = jarFileName.substring(start, jarFileName.length() - ".jar".length());

        // find first occurrence of -${NUMBER}. or -${NUMBER}$
        Matcher matcher = DASH_VERSION.matcher(name);
        if (matcher.find()) {
            name = name.substring(0, matcher.start());
        }

        name = NON_ALPHANUM.matcher(name).replaceAll(".");
        name = REPEATING_DOTS.matcher(name).replaceAll(".");

        // drop trailing dots
        int len = name.length();
        if (len > 0 && name.charAt(len - 1) == '.') {
            name = TRAILING_DOTS.matcher(name).replaceAll("");
        }

        return name;
    }

    public static String readModuleName(InputStream moduleInfo) throws IOException {
        // TODO: Support Java 8
        return ModuleDescriptor.read(moduleInfo).name();
    }
}
