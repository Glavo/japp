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
import org.glavo.japp.util.ByteBufferOutputStream;
import org.glavo.japp.util.ByteBufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class JAppConfigGroup {

    public enum Field {
        END,
        CONDITION,
        MAIN_CLASS,
        MAIN_MODULE,
        MODULE_PATH,
        CLASS_PATH,
        JVM_PROPERTIES,
        ADD_READS,
        ADD_EXPORTS,
        ADD_OPENS,
        ENABLE_NATIVE_ACCESS,
        SUB_GROUPS;

        private static final Field[] VALUES = values();

        public static Field readFrom(ByteBuffer buffer) throws IOException {
            byte id = buffer.get();
            if (id >= 0 && id < VALUES.length) {
                return VALUES[id];
            }
            throw new IOException(String.format("Unknown field: 0x%02x", Byte.toUnsignedInt(id)));
        }

        public byte id() {
            return (byte) ordinal();
        }
    }

    public static final int MAGIC_NUMBER = 0x00505247;

    public static JAppConfigGroup readFrom(ByteBuffer buffer) throws IOException {
        JAppConfigGroup group = new JAppConfigGroup();

        int magic = buffer.getInt();
        if (magic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong group magic: 0x%02x", magic));
        }

        Field field;
        while ((field = Field.readFrom(buffer)) != Field.END) {
            switch (field) {
                case CONDITION: {
                    if (group.condition != null) {
                        throw new IOException();
                    }
                    group.condition = ByteBufferUtils.readString(buffer);
                    break;
                }
                case MAIN_CLASS: {
                    if (group.mainClass != null) {
                        throw new IOException();
                    }
                    group.mainClass = ByteBufferUtils.readString(buffer);
                    break;
                }
                case MAIN_MODULE: {
                    if (group.mainModule != null) {
                        throw new IOException();
                    }
                    group.mainModule = ByteBufferUtils.readString(buffer);
                    break;
                }
                case JVM_PROPERTIES:
                case ADD_READS:
                case ADD_EXPORTS:
                case ADD_OPENS:
                case ENABLE_NATIVE_ACCESS: {
                    List<String> list;
                    if (field == Field.JVM_PROPERTIES) {
                        list = group.jvmProperties;
                    } else if (field == Field.ADD_READS) {
                        list = group.addReads;
                    } else if (field == Field.ADD_EXPORTS) {
                        list = group.addExports;
                    } else if (field == Field.ADD_OPENS) {
                        list = group.addOpens;
                    } else if (field == Field.ENABLE_NATIVE_ACCESS) {
                        list = group.enableNativeAccess;
                    } else {
                        throw new AssertionError("Field: " + field);
                    }
                    ByteBufferUtils.readStringList(buffer, list);
                    break;
                }
                case CLASS_PATH:
                case MODULE_PATH: {
                    List<JAppResourceGroupReference> list = field == Field.MODULE_PATH ? group.modulePath : group.classPath;

                    int count = buffer.getInt();
                    for (int i = 0; i < count; i++) {
                        list.add(JAppResourceGroupReference.readFrom(buffer));
                    }
                    break;
                }
                case SUB_GROUPS: {
                    int count = buffer.getInt();
                    for (int i = 0; i < count; i++) {
                        group.subGroups.add(readFrom(buffer));
                    }
                    break;
                }
                default:
                    throw new AssertionError("Field: " + field);
            }
        }

        return group;
    }

    public final List<JAppResourceGroupReference> modulePath = new ArrayList<>();
    public final List<JAppResourceGroupReference> classPath = new ArrayList<>();

    public final List<String> jvmProperties = new ArrayList<>();
    public final List<String> addReads = new ArrayList<>();
    public final List<String> addExports = new ArrayList<>();
    public final List<String> addOpens = new ArrayList<>();
    public final List<String> enableNativeAccess = new ArrayList<>();

    public String condition;

    public final List<JAppConfigGroup> subGroups = new ArrayList<>();

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

    private static void writeReferencesField(ByteBufferOutputStream out, Field field, List<JAppResourceGroupReference> list) throws IOException {
        if (list.isEmpty()) {
            return;
        }

        out.writeByte(field.id());
        out.writeInt(list.size());
        for (JAppResourceGroupReference reference : list) {
            reference.writeTo(out);
        }
    }

    private static void writeStringField(ByteBufferOutputStream out, Field field, String string) throws IOException {
        if (string == null) {
            return;
        }

        out.writeByte(field.id());
        out.writeString(string);
    }

    private static void writeStringListField(ByteBufferOutputStream out, Field field, List<String> list) throws IOException {
        if (list.isEmpty()) {
            return;
        }

        out.writeByte(field.id());
        out.writeInt(list.size());
        for (String string : list) {
            out.writeString(string);
        }
    }

    public void writeTo(ByteBufferOutputStream out) throws IOException {
        out.writeInt(MAGIC_NUMBER);

        writeReferencesField(out, Field.MODULE_PATH, modulePath);
        writeReferencesField(out, Field.CLASS_PATH, classPath);

        writeStringField(out, Field.CONDITION, condition);
        writeStringField(out, Field.MAIN_CLASS, mainClass);
        writeStringField(out, Field.MAIN_MODULE, mainModule);

        writeStringListField(out, Field.JVM_PROPERTIES, jvmProperties);
        writeStringListField(out, Field.ADD_READS, addReads);
        writeStringListField(out, Field.ADD_EXPORTS, addExports);
        writeStringListField(out, Field.ADD_OPENS, addOpens);
        writeStringListField(out, Field.ENABLE_NATIVE_ACCESS, enableNativeAccess);

        if (!subGroups.isEmpty()) {
            out.writeByte(Field.SUB_GROUPS.id());
            out.writeInt(subGroups.size());
            for (JAppConfigGroup subGroup : subGroups) {
                subGroup.writeTo(out);
            }
        }

        out.writeByte(Field.END.id());
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

            for (JAppConfigGroup subConfig : source.subGroups) {
                resolve(context, subConfig);
            }
        }
    }

    @Visibility(Visibility.Context.LAUNCHER)
    public void resolve(JAppRuntimeContext context) {
        for (JAppConfigGroup group : subGroups) {
            resolve(context, group);
        }
    }

}
