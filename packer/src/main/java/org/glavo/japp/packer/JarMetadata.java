package org.glavo.japp.packer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.attribute.FileTime;
import java.util.*;

public final class JarMetadata {
    private final String fileName;
    private final String moduleName;

    private final List<Entry> entries = new ArrayList<>();
    private Map<Integer, List<Entry>> multiReleaseEntries;

    public JarMetadata(String fileName, String moduleName) {
        this.fileName = fileName;
        this.moduleName = moduleName;
    }

    public List<Entry> getEntries() {
        return this.entries;
    }

    public List<Entry> getMultiReleaseEntries(int release) {
        if (multiReleaseEntries == null) {
            multiReleaseEntries = new TreeMap<>();
        }

        return multiReleaseEntries.computeIfAbsent(release, i -> new ArrayList<>());
    }


    public JSONObject toJson() {
        JSONObject res = new JSONObject();

        res.putOpt("File-Name", fileName);
        res.putOpt("Module-Name", moduleName);

        JSONArray jsonEntries = new JSONArray(this.entries.size());
        for (Entry entry : this.entries) {
            jsonEntries.put(entry.toJsonObject());
        }
        res.put("Entries", entries);

        if (multiReleaseEntries != null) {
            JSONObject multiRelease = new JSONObject();

            multiReleaseEntries.forEach((release, entries) -> {
                JSONArray releaseEntries = new JSONArray();
                for (Entry entry : this.entries) {
                    releaseEntries.put(entry.toJsonObject());
                }
                multiRelease.put(release.toString(), releaseEntries);
            });

            res.put("Multi-Release", multiRelease);
        }

        return res;
    }

    public static final class Entry {
        private final String name;
        private final long offset;
        private final long size;
        private final FileTime creationTime;
        private final FileTime lastModifiedTime;

        public Entry(String name, long offset, long size, FileTime creationTime, FileTime lastModifiedTime) {
            this.name = name;
            this.offset = offset;
            this.size = size;
            this.creationTime = creationTime;
            this.lastModifiedTime = lastModifiedTime;
        }

        JSONObject toJsonObject() {
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("Name", name);
            jsonEntry.put("Offset", offset);
            jsonEntry.put("Size", size);
            jsonEntry.put("Creation-Time", creationTime.toMillis());
            jsonEntry.put("Last-Modified-Time", lastModifiedTime.toMillis());
            return jsonEntry;
        }
    }
}
