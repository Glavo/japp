package org.glavo.japp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class JAppClasspathItem {
    private final String fileName;
    private final String moduleName;

    private final List<JAppResource> entries = new ArrayList<>();
    private Map<Integer, List<JAppResource>> multiReleaseEntries;

    public JAppClasspathItem(String fileName, String moduleName) {
        this.fileName = fileName;
        this.moduleName = moduleName;
    }

    public List<JAppResource> getEntries() {
        return this.entries;
    }

    public List<JAppResource> getMultiReleaseEntries(int release) {
        if (multiReleaseEntries == null) {
            multiReleaseEntries = new TreeMap<>();
        }

        return multiReleaseEntries.computeIfAbsent(release, i -> new ArrayList<>());
    }

    private static JSONArray writeEntries(List<JAppResource> entries) {
        JSONArray jsonEntries = new JSONArray(entries.size());
        for (JAppResource entry : entries) {

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

    private static void readEntries(List<JAppResource> entries, JSONArray jsonArray) {
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

            entries.add(new JAppResource(
                    name, offset, size,
                    creationTime > 0 ? FileTime.fromMillis(creationTime) : null,
                    lastModifiedTime > 0 ? FileTime.fromMillis(lastModifiedTime) : null
            ));
        }
    }

    public static JAppClasspathItem fromJson(JSONObject obj) {
        try {
            String fileName = (String) obj.opt("File-Name");
            String moduleName = (String) obj.opt("Module-Name");

            JAppClasspathItem item = new JAppClasspathItem(moduleName, fileName);

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
