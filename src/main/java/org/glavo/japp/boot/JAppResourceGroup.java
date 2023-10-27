package org.glavo.japp.boot;

import org.glavo.japp.JAppRuntimeContext;
import org.glavo.japp.annotation.Visibility;
import org.glavo.japp.thirdparty.json.JSONArray;
import org.glavo.japp.thirdparty.json.JSONObject;

import java.util.*;
import java.util.stream.Stream;

public final class JAppResourceGroup {

    String name;

    final Map<String, JAppResource> resources = new LinkedHashMap<>();
    SortedMap<Integer, Map<String, JAppResource>> multiReleaseResources;

    public JAppResourceGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<String, JAppResource> getResources() {
        return this.resources;
    }

    @Visibility(Visibility.Context.PACKER)
    public SortedMap<Integer, Map<String, JAppResource>> getMultiReleaseResources() {
        return multiReleaseResources;
    }


    @Visibility(Visibility.Context.PACKER)
    public Map<String, JAppResource> getMultiRelease(int release) {
        if (multiReleaseResources == null) {
            multiReleaseResources = new TreeMap<>();
        }

        return multiReleaseResources.computeIfAbsent(release, i -> new LinkedHashMap<>());
    }

    @Visibility(Visibility.Context.PACKER)
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
            jsonArray.put(resource.toJson());
        }
        return jsonArray;
    }

    public static JAppResourceGroup fromJson(JSONObject obj, JAppRuntimeContext context) {
        String name = obj.optString("Name");

        JAppResourceGroup res = new JAppResourceGroup(name);

        readResources(res.resources, (JSONArray) obj.opt("Resources"));

        JSONObject multiRelease = (JSONObject) obj.opt("Multi-Release");
        if (multiRelease != null) {
            String[] keys = new String[multiRelease.length()];
            Iterator<String> it = multiRelease.keys();
            for (int i = 0; i < keys.length; i++) {
                keys[i] = it.next();
            }
            Arrays.sort(keys, Comparator.comparing(Integer::parseInt));

            if (context == null) {
                for (String release : keys) {
                    readResources(res.getMultiRelease(Integer.parseInt(release)), multiRelease.getJSONArray(release));
                }
            } else {
                for (String release : keys) {
                    if (Integer.parseInt(release) > context.getRelease()) {
                        break;
                    }
                    readResources(res.resources, multiRelease.getJSONArray(release));
                }
            }
        }

        return res;
    }

    private static void readResources(Map<String, JAppResource> entries, JSONArray jsonArray) {
        if (jsonArray == null) {
            return;
        }

        for (Object o : jsonArray) {
            JAppResource resource = JAppResource.fromJson((JSONObject) o);
            entries.put(resource.getName(), resource);
        }
    }

    public JSONObject toJson() {
        JSONObject res = new JSONObject();

        res.put("Name", name);
        res.put("Resources", resourcesToJson(resources.values()));

        if (multiReleaseResources != null) {
            JSONObject multiRelease = new JSONObject();
            multiReleaseResources.forEach((release, entries) -> multiRelease.put(release.toString(), resourcesToJson(entries.values())));
            res.put("Multi-Release", multiRelease);
        }

        return res;
    }

    public Stream<JAppResource> list(int release) {
        if (multiReleaseResources == null) {
            return resources.values().stream();
        }

        LinkedHashMap<String, JAppResource> map = new LinkedHashMap<>(resources);
        multiReleaseResources.forEach((r, m) -> {
            if (r <= release) {
                map.putAll(m);
            }
        });

        return map.values().stream();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

}
