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
package org.glavo.japp.boot.module;

import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.boot.JAppReader;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.boot.JAppResourceRoot;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.stream.Stream;

public final class JAppModuleReference extends ModuleReference implements ModuleReader {

    private final JAppReader reader;
    private final JAppResourceGroup group;

    public JAppModuleReference(JAppReader reader, ModuleDescriptor descriptor, JAppResourceGroup group) {
        super(descriptor, JAppResourceRoot.MODULES.toURI(group));

        this.reader = reader;
        this.group = group;
    }

    @Override
    public ModuleReader open() throws IOException {
        return this;
    }

    // ModuleReader

    @Override
    public Optional<URI> find(String name) throws IOException {
        JAppResource resource = group.get(name);
        if (resource != null) {
            return Optional.of(JAppResourceRoot.MODULES.toURI(group, resource));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<InputStream> open(String name) throws IOException {
        JAppResource resource = group.get(name);
        if (resource != null) {
            return Optional.of(reader.openResource(resource));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ByteBuffer> read(String name) throws IOException {
        JAppResource resource = group.get(name);
        if (resource != null) {
            return Optional.of(reader.readResource(resource));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Stream<String> list() throws IOException {
        return group.values().stream().map(JAppResource::getName);
    }

    @Override
    public void close() throws IOException {
    }
}
