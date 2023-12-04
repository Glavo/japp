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

import org.glavo.japp.JAppResourceGroupReference;
import org.glavo.japp.packer.compressor.CompressResult;
import org.glavo.japp.util.XxHash64;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class JAppResourcesWriter implements AutoCloseable {
    private final JAppPacker packer;
    private final String name;
    private final boolean isModulePath;

    private final Map<String, JAppResourceInfo> resources = new LinkedHashMap<>();
    private final Map<Integer, Map<String, JAppResourceInfo>> multiReleaseResources = new TreeMap<>();

    JAppResourcesWriter(JAppPacker packer, String name, boolean isModulePath) {
        this.packer = packer;
        this.name = name;
        this.isModulePath = isModulePath;
    }

    public void writeResource(JAppResourceInfo resource, byte[] body) throws IOException {
        writeResource(-1, resource, body);
    }

    public void writeResource(int release, JAppResourceInfo resource, byte[] body) throws IOException {
        if (resource.hasWritten) {
            throw new AssertionError("Resource " + resource.name + " has been written");
        }

        resource.hasWritten = true;

        Map<String, JAppResourceInfo> resources;
        if (release == -1) {
            resources = this.resources;
        } else {
            resources = this.multiReleaseResources.computeIfAbsent(release, r -> new LinkedHashMap<>());
        }

        resources.put(resource.name, resource);
        resource.offset = packer.getCurrentOffset();
        resource.size = body.length;
        resource.checksum = XxHash64.hash(body);

        CompressResult result = packer.compressor.compress(packer, body, resource.name);
        resource.method = result.getMethod();
        resource.compressedSize = result.getLength();

        packer.getOutput().writeBytes(result.getCompressedData(), result.getOffset(), result.getLength());
    }

    private int addGroup(Map<String, JAppResourceInfo> group) {
        int index = packer.groups.size();
        packer.groups.add(group);
        return index;
    }

    public void close() {
        int baseIndex = addGroup(resources);
        TreeMap<Integer, Integer> multiIndexes;
        if (!multiReleaseResources.isEmpty()) {
            multiIndexes = new TreeMap<>();
            multiReleaseResources.forEach((i, g) -> multiIndexes.put(i, addGroup(g)));
        } else {
            multiIndexes = null;
        }

        packer.addReference(new JAppResourceGroupReference.Local(name, baseIndex, multiIndexes), isModulePath);
    }
}
