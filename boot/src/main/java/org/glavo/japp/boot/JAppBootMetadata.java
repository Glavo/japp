package org.glavo.japp.boot;

import org.glavo.japp.boot.decompressor.classfile.ByteArrayPool;
import org.glavo.japp.json.JSONArray;
import org.glavo.japp.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public final class JAppBootMetadata {
    public static JAppBootMetadata fromJson(JSONObject obj) throws IOException {
        JSONArray array = obj.getJSONArray("Groups");
        JAppResourceGroup[] groups = new JAppResourceGroup[array.length()];
        for (int i = 0; i < groups.length; i++) {
            groups[i] = JAppResourceGroup.fromJson(array.getJSONArray(i));
        }

        ByteArrayPool pool = ByteArrayPool.readPool(Channels.newChannel(new ByteArrayInputStream(
                Base64.getDecoder().decode(obj.getString("Pool"))
        )));
        return new JAppBootMetadata(Arrays.asList(groups), pool);
    }

    public static JSONObject toJson(List<JAppResourceGroup> groups, byte[] pool) {
        JSONObject res = new JSONObject();

        JSONArray groupsArray = new JSONArray();
        for (JAppResourceGroup group : groups) {
            groupsArray.put(group.toJson());
        }
        res.put("Groups", groupsArray);
        res.put("Pool", Base64.getEncoder().encodeToString(pool));

        return res;
    }

    private final List<JAppResourceGroup> groups;
    private final ByteArrayPool pool;

    public JAppBootMetadata(List<JAppResourceGroup> groups, ByteArrayPool pool) {
        this.groups = groups;
        this.pool = pool;
    }

    public List<JAppResourceGroup> getGroups() {
        return groups;
    }

    public ByteArrayPool getPool() {
        return pool;
    }
}
