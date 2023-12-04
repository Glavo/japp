/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.boot;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public enum JAppResourceRoot {
    MODULES, CLASSPATH, RESOURCE;

    private final String rootName;
    private final String pathPrefix;

    JAppResourceRoot() {
        this.rootName = name().toLowerCase(Locale.ROOT);
        this.pathPrefix = '/' + rootName;
    }

    public String getRootName() {
        return rootName;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public URI toURI(JAppResourceGroup group) {
        try {
            return new URI("japp", null, pathPrefix + '/' + group.getName() + '/', null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URI toURI(JAppResourceGroup group, JAppResource resource) {
        try {
            return new URI("japp", null, pathPrefix + '/' + group.getName() + '/' + resource.getName(), null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
