/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp;

import org.glavo.japp.annotation.Visibility;
import org.glavo.japp.condition.ConditionParser;
import org.glavo.japp.platform.JAppRuntimeContext;
import org.glavo.japp.json.JSONArray;
import org.glavo.japp.json.JSONObject;
import org.glavo.japp.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class JAppConfigGroup {

    public static JAppConfigGroup readConfigGroup(ByteBuffer buffer) throws IOException {
        return JAppConfigGroup.fromJson(new JSONObject(new String(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining(), UTF_8)));
    }

    public final List<JAppResourceGroupReference> modulePath = new ArrayList<>();
    public final List<JAppResourceGroupReference> classPath = new ArrayList<>();

    public final List<String> jvmProperties = new ArrayList<>();
    public final List<String> addReads = new ArrayList<>();
    public final List<String> addExports = new ArrayList<>();
    public final List<String> addOpens = new ArrayList<>();
    public final List<String> enableNativeAccess = new ArrayList<>();

    public String condition;

    public final List<JAppConfigGroup> subConfigs = new ArrayList<>();

    public String mainClass;
    public String mainModule;

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

    public List<JAppResourceGroupReference> getModulePath() {
        return modulePath;
    }

    public List<JAppResourceGroupReference> getClassPath() {
        return classPath;
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

    private void readReferences(boolean isModulePath, JSONArray array) throws IOException {
        List<JAppResourceGroupReference> list = isModulePath ? this.modulePath : this.classPath;

        if (array != null) {
            for (Object jsonItem : array) {
                list.add(JAppResourceGroupReference.fromJson((JSONObject) jsonItem));
            }
        }
    }

    public static JAppConfigGroup fromJson(JSONObject obj) throws IOException {
        JAppConfigGroup config = new JAppConfigGroup();

        config.readReferences(false, obj.optJSONArray("Class-Path"));
        config.readReferences(true, obj.optJSONArray("Module-Path"));

        readJsonArray(config.jvmProperties, obj, "Properties");
        readJsonArray(config.addReads, obj, "Add-Reads");
        readJsonArray(config.addExports, obj, "Add-Exports");
        readJsonArray(config.addOpens, obj, "Add-Opens");
        readJsonArray(config.enableNativeAccess, obj, "Enable-Native-Access");

        config.condition = obj.optString("Condition", null);
        config.mainClass = obj.optString("Main-Class", null);
        config.mainModule = obj.optString("Main-Module", null);

        JSONArray subGroups = obj.optJSONArray("Groups");
        if (subGroups != null) {
            for (Object group : subGroups) {
                config.subConfigs.add(fromJson((JSONObject) group));
            }
        }

        return config;
    }

    public JSONObject toJson() {
        JSONObject res = new JSONObject();

        JSONArray modulePath = new JSONArray();
        JSONArray classPath = new JSONArray();

        for (JAppResourceGroupReference reference : this.modulePath) {
            modulePath.put(reference.toJson());
        }

        for (JAppResourceGroupReference reference : this.classPath) {
            classPath.put(reference.toJson());
        }

        res.put("Module-Path", modulePath);
        res.put("Class-Path", classPath);

        putJsonArray(res, "Properties", jvmProperties);
        putJsonArray(res, "Add-Reads", addReads);
        putJsonArray(res, "Add-Exports", addExports);
        putJsonArray(res, "Add-Opens", addOpens);
        putJsonArray(res, "Enable-Native-Access", enableNativeAccess);

        res.putOpt("Condition", condition);

        res.putOpt("Main-Class", mainClass);
        res.putOpt("Main-Module", mainModule);

        if (!subConfigs.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (JAppConfigGroup config : subConfigs) {
                arr.put(config.toJson());
            }
            res.put("Groups", arr);
        }

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

    @Visibility(Visibility.Context.LAUNCHER)
    public boolean canApply(JAppRuntimeContext context) {
        return condition == null || ConditionParser.parse(condition).test(context);
    }

    private void addOrReplace(List<JAppResourceGroupReference> target, List<JAppResourceGroupReference> source) {
        loop:
        for (JAppResourceGroupReference reference : source) {
            if (reference.name != null) {
                for (int i = 0; i < target.size(); i++) {
                    if (reference.name.equals(target.get(i).name)) {
                        target.set(i, reference);
                        continue loop;
                    }
                }
            }
            target.add(reference);
        }
    }

    @Visibility(Visibility.Context.LAUNCHER)
    private void resolve(JAppRuntimeContext context, JAppConfigGroup source) {
        if (source.canApply(context)) {
            addOrReplace(modulePath, source.modulePath);
            addOrReplace(classPath, source.classPath);
            jvmProperties.addAll(source.jvmProperties);
            addReads.addAll(source.addReads);
            addExports.addAll(source.addExports);
            addOpens.addAll(source.addOpens);
            enableNativeAccess.addAll(source.enableNativeAccess);

            if (source.mainModule != null) {
                mainModule = source.mainModule;
            }

            if (source.mainClass != null) {
                mainClass = source.mainClass;
            }

            for (JAppConfigGroup subConfig : source.subConfigs) {
                resolve(context, subConfig);
            }
        }
    }

    @Visibility(Visibility.Context.LAUNCHER)
    public void resolve(JAppRuntimeContext context) {
        for (JAppConfigGroup group : subConfigs) {
            resolve(context, group);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName() + toJson();
    }
}
