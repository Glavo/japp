package org.glavo.japp.boot;

import org.glavo.japp.json.JSONArray;
import org.glavo.japp.json.JSONObject;

import java.util.*;

public final class JAppResourceGroup extends LinkedHashMap<String, JAppResource> {

    static final byte MAGIC_NUMBER = (byte) 0xeb;
    static final int HEADER_LENGTH = 24; // 1 + 1 + 2 + 4 + 4 + 4 + 8

    private String name;

    public JAppResourceGroup() {
    }

    public JAppResourceGroup(String name) {
        this.name = name;
    }

    public void initName(String name) {
        if (this.name != null) {
            throw new IllegalStateException();
        }

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void readResources(JSONArray jsonArray) {
        if (jsonArray == null) {
            return;
        }

        for (Object o : jsonArray) {
            JAppResource resource = JAppResource.fromJson((JSONObject) o);
            put(resource.getName(), resource);
        }
    }

    public static JAppResourceGroup fromJson(JSONArray array) {
        JAppResourceGroup group = new JAppResourceGroup();
        group.readResources(array);
        return group;
    }

    public JSONArray toJson() {
        JSONArray jsonArray = new JSONArray(this.size());
        for (JAppResource resource : this.values()) {
            jsonArray.put(resource.toJson());
        }
        return jsonArray;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }

}
