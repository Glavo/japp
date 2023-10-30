package org.glavo.japp.boot;

import org.glavo.japp.launcher.condition.JAppRuntimeContext;
import org.glavo.japp.thirdparty.json.JSONArray;
import org.glavo.japp.thirdparty.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public final class JAppBootMetadata {
    public static JAppBootMetadata fromJson(JSONObject obj, int release) {
        JSONArray array = obj.getJSONArray("Groups");
        JAppResourceGroup[] groups = new JAppResourceGroup[array.length()];
        for (int i = 0; i < groups.length; i++) {
            groups[i] = JAppResourceGroup.fromJson(array.getJSONObject(i), release);
        }
        return new JAppBootMetadata(Arrays.asList(groups));
    }

    private final List<JAppResourceGroup> groups;

    public JAppBootMetadata(List<JAppResourceGroup> groups) {
        this.groups = groups;
    }

    public List<JAppResourceGroup> getGroups() {
        return groups;
    }

    public JSONObject toJson() {
        JSONObject res = new JSONObject();

        JSONArray groupsArray = new JSONArray();
        for (JAppResourceGroup group : groups) {
            groupsArray.put(group.toJson());
        }
        res.put("Groups", groupsArray);

        return res;
    }
}
