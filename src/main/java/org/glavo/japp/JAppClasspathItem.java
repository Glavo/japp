package org.glavo.japp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.file.attribute.FileTime;
import java.util.*;

public final class JAppClasspathItem {
    private final String name;

    private final Map<String, JAppResource> resources = new LinkedHashMap<>();
    private SortedMap<Integer, Map<String, JAppResource>> multiReleaseResources;

    public JAppClasspathItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Collection<JAppResource> getResources() {
        return this.resources.values();
    }

    public Map<String, JAppResource> getMultiRelease(int release) {
        if (multiReleaseResources == null) {
            multiReleaseResources = new TreeMap<>();
        }

        return multiReleaseResources.computeIfAbsent(release, i -> new LinkedHashMap<>());
    }

    public JAppResource findResource(int release, String path) {
        JAppResource resource = null;

        if (multiReleaseResources != null) {
            for (Map.Entry<Integer, Map<String, JAppResource>> pair : multiReleaseResources.entrySet()) {
                if (pair.getKey() <= release) {
                    JAppResource r = pair.getValue().get(path);
                    if (r != null) {
                        resource = r;
                    }
                }
            }
        }

        if (resource == null) {
            resource = resources.get(path);
        }

        return resource;
    }

    private static JSONArray resourcesToJson(Collection<JAppResource> resources) {
        if (resources == null) {
            return new JSONArray();
        }

        JSONArray jsonArray = new JSONArray(resources.size());
        for (JAppResource resource : resources) {
            JSONObject obj = new JSONObject();

            obj.putOnce("Name", resource.getName());
            obj.putOnce("Offset", resource.getOffset());
            obj.putOnce("Size", resource.getSize());
            obj.putOnce("Creation-Time", resource.getCreationTime().toMillis());
            obj.putOnce("Last-Modified-Time", resource.getLastModifiedTime().toMillis());

            jsonArray.put(obj);
        }
        return jsonArray;
    }

    public JSONObject toJson() {
        JSONObject res = new JSONObject();

        res.putOpt("Name", name);
        res.put("Resources", resourcesToJson(resources.values()));

        if (multiReleaseResources != null) {
            JSONObject multiRelease = new JSONObject();
            multiReleaseResources.forEach((release, entries) -> multiRelease.put(release.toString(), resourcesToJson(entries.values())));
            res.put("Multi-Release", multiRelease);
        }

        return res;
    }

    private static void readResources(Map<String, JAppResource> entries, JSONArray jsonArray) {
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

            entries.put(name, new JAppResource(
                    name, offset, size,
                    creationTime > 0 ? FileTime.fromMillis(creationTime) : null,
                    lastModifiedTime > 0 ? FileTime.fromMillis(lastModifiedTime) : null
            ));
        }
    }

    public static JAppClasspathItem fromJson(JSONObject obj) {
        try {
            JAppClasspathItem item = new JAppClasspathItem((String) obj.opt("Name"));
            readResources(item.resources, (JSONArray) obj.opt("Resources"));

            JSONObject multiRelease = (JSONObject) obj.opt("Multi-Release");
            if (multiRelease != null) {
                multiRelease.keys().forEachRemaining(release ->
                        readResources(item.getMultiRelease(Integer.parseInt(release)), multiRelease.getJSONArray(release))
                );
            }

            return item;
        } catch (ClassCastException | NumberFormatException | JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
