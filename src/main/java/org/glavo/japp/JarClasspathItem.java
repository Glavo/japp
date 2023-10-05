package org.glavo.japp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.file.attribute.FileTime;
import java.util.*;

public final class JarClasspathItem implements ClasspathItem {
    private final String fileName;
    private final String moduleName;

    private final List<JAppEntry> entries = new ArrayList<>();
    private Map<Integer, List<JAppEntry>> multiReleaseEntries;

    public JarClasspathItem(String fileName, String moduleName) {
        this.fileName = fileName;
        this.moduleName = moduleName;
    }

    public List<JAppEntry> getEntries() {
        return this.entries;
    }

    public List<JAppEntry> getMultiReleaseEntries(int release) {
        if (multiReleaseEntries == null) {
            multiReleaseEntries = new TreeMap<>();
        }

        return multiReleaseEntries.computeIfAbsent(release, i -> new ArrayList<>());
    }

    private static JSONArray writeEntries(List<JAppEntry> entries) {
        JSONArray jsonEntries = new JSONArray(entries.size());
        for (JAppEntry entry : entries) {
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("Name", entry.name);
            jsonEntry.put("Offset", entry.offset);
            jsonEntry.put("Size", entry.size);
            if (entry.creationTime != null) {
                jsonEntry.put("Creation-Time", entry.creationTime.toMillis());
            }
            if (entry.lastModifiedTime != null) {
                jsonEntry.put("Last-Modified-Time", entry.lastModifiedTime.toMillis());
            }
        }
        return jsonEntries;
    }

    public JSONObject toJson() {
        JSONObject res = new JSONObject();

        res.putOpt("File-Name", fileName);
        res.putOpt("Module-Name", moduleName);
        res.put("Entries", writeEntries(this.entries));

        if (multiReleaseEntries != null) {
            JSONObject multiRelease = new JSONObject();
            multiReleaseEntries.forEach((release, entries) -> multiRelease.put(release.toString(), writeEntries(entries)));
            res.put("Multi-Release", multiRelease);
        }

        return res;
    }

    private static void readEntries(List<JAppEntry> entries, JSONArray jsonArray) {
        if (jsonArray == null) {
            return;
        }

        for (Object o : jsonArray) {
            JSONObject jsonEntry = (JSONObject) o;
            String name = jsonEntry.getString("Name");
            long offset = jsonEntry.getLong("Offset");
            long size = jsonEntry.getLong("Size");
            long creationTime = jsonEntry.optLong("Creation-Time", -1L);
            long lastModifiedTime = jsonEntry.optLong("Last-Modified-Time", -1L);

            entries.add(new JAppEntry(
                    name, offset, size,
                    creationTime > 0 ? FileTime.fromMillis(creationTime) : null,
                    lastModifiedTime > 0 ? FileTime.fromMillis(lastModifiedTime) : null
            ));
        }
    }

    public static JarClasspathItem fromJson(JSONObject obj) {
        try {
            String fileName = (String) obj.opt("File-Name");
            String moduleName = (String) obj.opt("Module-Name");

            JarClasspathItem item = new JarClasspathItem(moduleName, fileName);

            readEntries(item.getEntries(), (JSONArray) obj.opt("Entries"));

            JSONObject multiRelease = (JSONObject) obj.opt("Multi-Release");
            if (multiRelease != null) {
                multiRelease.keys().forEachRemaining(release ->
                        readEntries(item.getMultiReleaseEntries(Integer.parseInt(release)), multiRelease.getJSONArray(release))
                );
            }

            return item;
        } catch (ClassCastException | NumberFormatException | JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
