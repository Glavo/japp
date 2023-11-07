package org.glavo.japp.launcher;

import org.glavo.japp.annotation.Visibility;
import org.glavo.japp.launcher.condition.ConditionParser;
import org.glavo.japp.launcher.condition.JAppRuntimeContext;
import org.glavo.japp.thirdparty.json.JSONArray;
import org.glavo.japp.thirdparty.json.JSONObject;
import org.glavo.japp.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class JAppConfigGroup {

    public static final short MAJOR_VERSION = -1;
    public static final short MINOR_VERSION = 0;

    public static final int FILE_END_SIZE = 48;

    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    public static JAppConfigGroup readFile(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file)) {
            long fileSize = channel.size();

            if (fileSize < FILE_END_SIZE) {
                throw new IOException("File is too small");
            }

            int endBufferSize = (int) Math.min(fileSize, 8192);
            ByteBuffer endBuffer = ByteBuffer.allocate(endBufferSize).order(ByteOrder.LITTLE_ENDIAN);

            channel.position(fileSize - endBufferSize);

            IOUtils.readFully(channel, endBuffer);

            endBuffer.limit(endBufferSize).position(endBufferSize - FILE_END_SIZE);

            int magicNumber = endBuffer.getInt();
            if (magicNumber != 0x5050414a) {
                throw new IOException("Invalid magic number: " + Long.toHexString(magicNumber));
            }

            short majorVersion = endBuffer.getShort();
            short minorVersion = endBuffer.getShort();

            if (majorVersion != MAJOR_VERSION || minorVersion != MINOR_VERSION) {
                throw new IOException("Version number mismatch");
            }

            long flags = endBuffer.getLong();

            long fileContentSize = endBuffer.getLong();
            long metadataOffset = endBuffer.getLong();
            long bootMetadataOffset = endBuffer.getLong();

            assert endBuffer.remaining() == 8;

            if (flags != 0) {
                throw new IOException("Unsupported flags: " + Long.toBinaryString(flags));
            }

            if (fileContentSize > fileSize || fileContentSize < FILE_END_SIZE) {
                throw new IOException("Invalid file size: " + fileContentSize);
            }

            if (metadataOffset >= fileContentSize - FILE_END_SIZE) {
                throw new IOException("Invalid metadata offset: " + metadataOffset);
            }

            long baseOffset = fileSize - fileContentSize;
            long metadataSize = fileContentSize - FILE_END_SIZE - metadataOffset;

            String json;
            if (metadataSize < endBufferSize - FILE_END_SIZE) {
                json = new String(endBuffer.array(), (int) (endBufferSize - metadataSize - FILE_END_SIZE), (int) metadataSize, UTF_8);
            } else {
                if (metadataSize > (1 << 30)) {
                    throw new IOException("Metadata is too large");
                }

                ByteBuffer metadataBuffer = ByteBuffer.allocate((int) metadataSize);
                channel.position(baseOffset + metadataOffset);
                IOUtils.readFully(channel, metadataBuffer);

                json = new String(metadataBuffer.array(), UTF_8);
            }

            JAppConfigGroup metadata = JAppConfigGroup.fromJson(new JSONObject(json));
            metadata.baseOffset = baseOffset;
            metadata.bootMetadataOffset = bootMetadataOffset;
            return metadata;
        }
    }

    private long baseOffset;
    private long bootMetadataOffset;

    public final List<JAppResourceReference> modulePath = new ArrayList<>();
    public final List<JAppResourceReference> classPath = new ArrayList<>();

    public final List<String> jvmProperties = new ArrayList<>();
    public final List<String> addReads = new ArrayList<>();
    public final List<String> addExports = new ArrayList<>();
    public final List<String> addOpens = new ArrayList<>();
    public final List<String> enableNativeAccess = new ArrayList<>();

    public String condition;

    public final List<JAppConfigGroup> subConfigs =  new ArrayList<>();

    public String mainClass;
    public String mainModule;

    public long getBaseOffset() {
        return baseOffset;
    }

    public long getBootMetadataOffset() {
        return bootMetadataOffset;
    }

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

    public List<JAppResourceReference> getModulePath() {
        return modulePath;
    }

    public List<JAppResourceReference> getClassPath() {
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
        List<JAppResourceReference> list = isModulePath ? this.modulePath : this.classPath;

        if (array != null) {
            for (Object jsonItem : array) {
                list.add(JAppResourceReference.fromJson((JSONObject) jsonItem));
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

        for (JAppResourceReference reference : this.modulePath) {
            modulePath.put(reference.toJson());
        }

        for (JAppResourceReference reference : this.classPath) {
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

    private void addOrReplace(List<JAppResourceReference> target, List<JAppResourceReference> source) {
        loop:
        for (JAppResourceReference reference : source) {
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
