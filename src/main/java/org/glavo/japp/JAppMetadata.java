package org.glavo.japp;

import org.glavo.japp.condition.Condition;
import org.glavo.japp.thirdparty.json.JSONArray;
import org.glavo.japp.thirdparty.json.JSONObject;
import org.glavo.japp.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class JAppMetadata {

    public static final short MAJOR_VERSION = -1;
    public static final short MINOR_VERSION = 0;

    public static final int FILE_END_SIZE = 48;

    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    public static JAppMetadata readFile(Path file) throws IOException {
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

            assert endBuffer.remaining() == 16;

            if (flags != 0) {
                throw new IOException("Unsupported flags: " + Long.toBinaryString(flags));
            }

            if (fileContentSize > fileSize || fileContentSize < FILE_END_SIZE) {
                throw new IOException("Invalid file size: " + fileContentSize);
            }

            if (metadataOffset >= fileContentSize - FILE_END_SIZE) {
                throw new IOException("Invalid metadata offset: " + metadataOffset);
            }

            long contentOffset = fileSize - fileContentSize;
            long metadataSize = fileContentSize - FILE_END_SIZE - metadataOffset;

            String json;
            if (metadataSize < endBufferSize - FILE_END_SIZE) {
                json = new String(endBuffer.array(), (int) (endBufferSize - metadataSize - FILE_END_SIZE), (int) metadataSize, UTF_8);
            } else {
                if (metadataSize > (1 << 30)) {
                    throw new IOException("Metadata is too large");
                }

                ByteBuffer metadataBuffer = ByteBuffer.allocate((int) metadataSize);
                channel.position(contentOffset + metadataOffset);
                IOUtils.readFully(channel, metadataBuffer);

                json = new String(metadataBuffer.array(), UTF_8);
            }


            return JAppMetadata.fromJson(new JSONObject(json), null);
        }
    }

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
