package org.glavo.japp.packer;

import org.glavo.japp.launcher.JAppResourceReference;
import org.glavo.japp.packer.compressor.CompressResult;

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

        JAppResourceReference.Local ref = new JAppResourceReference.Local(name, baseIndex, multiIndexes);
        if (isModulePath) {
            packer.current.getModulePath().add(ref);
        } else {
            packer.current.getClassPath().add(ref);
        }
    }
}
