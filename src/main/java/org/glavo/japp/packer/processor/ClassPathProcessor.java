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
package org.glavo.japp.packer.processor;

import org.glavo.japp.packer.JAppWriter;

import java.util.Map;

public abstract class ClassPathProcessor {
    private static ClassPathProcessor getProcessor(String type) {
        if (type == null) {
            return LocalClassPathProcessor.INSTANCE;
        }
        switch (type) {
            case "local":
                return LocalClassPathProcessor.INSTANCE;
            case "maven":
                return new MavenClassPathProcessor();
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public static void process(JAppWriter writer, String pathList, boolean isModulePath) throws Throwable {
        if (pathList == null || pathList.isEmpty()) {
            return;
        }

        PathListParser parser = new PathListParser(pathList);
        while (parser.scanNext()) {
            Map<String, String> options = parser.options;
            String path = parser.path;

            ClassPathProcessor processor = getProcessor(options.remove("type"));
            processor.process(writer, path, isModulePath, options);
        }
    }

    public abstract void process(JAppWriter packer, String path, boolean isModulePath, Map<String, String> options) throws Throwable;
}
