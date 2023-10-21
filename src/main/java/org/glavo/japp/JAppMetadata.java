package org.glavo.japp;

import org.glavo.japp.condition.Condition;
import org.glavo.japp.thirdparty.json.JSONArray;
import org.glavo.japp.thirdparty.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JAppMetadata {

    public static JAppMetadata fromJson(JSONObject obj, JAppRuntimeContext context) throws IOException {
        JAppMetadata metadata = new JAppMetadata();

        metadata.readClasspathItems(false, obj.optJSONArray("Class-Path"), context);
        metadata.readClasspathItems(true, obj.optJSONArray("Module-Path"), context);

        readJsonArray(metadata.jvmProperties, obj, "Properties");
        readJsonArray(metadata.addReads, obj, "Add-Reads");
        readJsonArray(metadata.addExports, obj, "Add-Exports");
        readJsonArray(metadata.addOpens, obj, "Add-Opens");
        readJsonArray(metadata.enableNativeAccess, obj, "Enable-Native-Access");

        metadata.mainClass = obj.optString("Main-Class");
        metadata.mainModule = obj.optString("Main-Module");

        return metadata;
    }

    private void readClasspathItems(boolean isModulePath, JSONArray array, JAppRuntimeContext context) throws IOException {
        Map<String, JAppClasspathItem> map = isModulePath ? this.modulePath : this.classPath;

        if (array != null) {
            for (Object jsonItem : array) {
                JAppClasspathItem item = JAppClasspathItem.fromJson(((JSONObject) jsonItem), context);
                String name = item.getName();

                if (name == null) {
                    throw new IOException("Item missing name");
                }

                if (map.put(name, item) != null) {
                    throw new IOException(String.format("Duplicate %s path item: %s", isModulePath ? "module" : "", name));
                }
            }
        }
    }

    private static void readJsonArray(List<String> list, JSONObject obj, String key) {
        JSONArray arr = obj.optJSONArray(key);
        if (arr == null) {
            return;
        }

        for (Object o : arr) {
            list.add((String) o);
        }
    }

    public enum SubMode {
        DEFAULT,
        FORCE,
        SWITCH,
        IGNORE
    }

    final Map<String, JAppClasspathItem> modulePath = new LinkedHashMap<>();
    final Map<String, JAppClasspathItem> classPath = new LinkedHashMap<>();

    final List<String> jvmProperties = new ArrayList<>();
    final List<String> addReads = new ArrayList<>();
    final List<String> addExports = new ArrayList<>();
    final List<String> addOpens = new ArrayList<>();
    final List<String> enableNativeAccess = new ArrayList<>();

    SubMode subMode;
    final List<JAppMetadata> subMetadata =  new ArrayList<>();

    Condition condition;

    String mainClass;
    String mainModule;

    public List<String> getJvmProperties() {
        return jvmProperties;
    }

    public List<String> getAddReads() {
        return addReads;
    }

    public List<String> getAddExports() {
        return addExports;
    }

    public List<String> getAddOpens() {
        return addOpens;
    }

    public List<String> getEnableNativeAccess() {
        return enableNativeAccess;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getMainModule() {
        return mainModule;
    }

    public Map<String, JAppClasspathItem> getModulePathItems() {
        return modulePath;
    }

    public Map<String, JAppClasspathItem> getClassPathItems() {
        return classPath;
    }

    public List<JAppMetadata> getSubMetadata() {
        return subMetadata;
    }

    public JSONObject toJson() {
        JSONObject res = new JSONObject();

        JSONArray modulePath = new JSONArray();
        JSONArray classPath = new JSONArray();

        for (JAppClasspathItem metadata : this.modulePath.values()) {
            modulePath.put(metadata.toJson());
        }

        for (JAppClasspathItem metadata : this.classPath.values()) {
            classPath.put(metadata.toJson());
        }

        res.put("Module-Path", modulePath);
        res.put("Class-Path", classPath);

        putJsonArray(res, "Properties", jvmProperties);
        putJsonArray(res, "Add-Reads", addReads);
        putJsonArray(res, "Add-Exports", addExports);
        putJsonArray(res, "Add-Opens", addOpens);
        putJsonArray(res, "Enable-Native-Access", enableNativeAccess);

        res.putOpt("Main-Class", mainClass);
        res.putOpt("Main-Module", mainModule);

        return res;
    }

    private static void putJsonArray(JSONObject obj, String key, List<String> list) {
        if (list.isEmpty()) {
            return;
        }
        JSONArray arr = new JSONArray();
        for (String s : list) {
            arr.put(s);
        }
        obj.put(key, arr);
    }
}
