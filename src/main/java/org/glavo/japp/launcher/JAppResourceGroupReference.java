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
package org.glavo.japp.launcher;

import org.glavo.japp.io.LittleEndianDataOutput;
import org.glavo.japp.util.ByteBufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

public abstract class JAppResourceGroupReference {

    public static final byte MAGIC_NUMBER = 0x01;

    protected JAppResourceGroupReference(String name) {
        this.name = name;
    }

    public static JAppResourceGroupReference readFrom(ByteBuffer buffer) throws IOException {
        byte magic = buffer.get();
        if (magic != MAGIC_NUMBER) {
            throw new IOException(String.format("Wrong reference magic: 0x%02x", Byte.toUnsignedInt(magic)));
        }

        byte id = buffer.get();
        String name = ByteBufferUtils.readString(buffer);
        switch (id) {
            case Local.ID: {
                int index = buffer.getInt();
                int multiCount = buffer.getInt();
                TreeMap<Integer, Integer> multiReleaseIndexes;
                if (multiCount == 0) {
                    multiReleaseIndexes = null;
                } else {
                    multiReleaseIndexes = new TreeMap<>();
                    for (int i = 0; i < multiCount; i++) {
                        int multiVersion = buffer.getInt();
                        int multiIndex = buffer.getInt();

                        if (multiVersion < 9) {
                            throw new IOException("Version should not less than 9");
                        }

                        if (multiReleaseIndexes.put(multiVersion, multiIndex) != null) {
                            throw new IOException("Duplicate version: " + multiVersion);
                        }
                    }
                }

                return new Local(name, index, multiReleaseIndexes);
            }
            case Maven.ID: {
                String repository = ByteBufferUtils.readStringOrNull(buffer);
                String group = ByteBufferUtils.readStringOrNull(buffer);
                String artifact = ByteBufferUtils.readStringOrNull(buffer);
                String version = ByteBufferUtils.readStringOrNull(buffer);
                String classifier = ByteBufferUtils.readStringOrNull(buffer);

                return new Maven(name, repository, group, artifact, version, classifier);
            }
            default:
                throw new IOException(String.format("Unknown reference id: 0x%02x", Byte.toUnsignedInt(id)));
        }
    }

    public final String name;

    public String getName() {
        return name;
    }

    public abstract void writeTo(LittleEndianDataOutput out) throws IOException;

    public static final class Local extends JAppResourceGroupReference {
        public static final byte ID = 0;

        private final int index;
        private final TreeMap<Integer, Integer> multiReleaseIndexes;

        public Local(String name, int index, TreeMap<Integer, Integer> multiReleaseIndexes) {
            super(name);
            if (index < 0) {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }

            this.index = index;
            this.multiReleaseIndexes = multiReleaseIndexes;
        }

        public int getIndex() {
            return index;
        }

        public TreeMap<Integer, Integer> getMultiReleaseIndexes() {
            return multiReleaseIndexes;
        }

        @Override
        public void writeTo(LittleEndianDataOutput out) throws IOException {
            out.writeByte(MAGIC_NUMBER);
            out.writeByte(ID);
            out.writeString(name);
            out.writeInt(index);

            if (multiReleaseIndexes == null) {
                out.writeInt(0);
            } else {
                out.writeInt(multiReleaseIndexes.size());
                for (Map.Entry<Integer, Integer> entry : multiReleaseIndexes.entrySet()) {
                    out.writeInt(entry.getKey());
                    out.writeInt(entry.getValue());
                }
            }
        }

        @Override
        public String toString() {
            if (multiReleaseIndexes == null) {
                return "Local[index=" + index + ']';
            } else {
                return String.format("Local[index=%d, multiReleaseIndexes=%s]", index, multiReleaseIndexes);
            }
        }
    }

    public static final class Maven extends JAppResourceGroupReference {
        public static final byte ID = 1;

        private final String repository;
        private final String group;
        private final String artifact;
        private final String version;
        private final String classifier;

        public Maven(String name, String repository, String group, String artifact, String version, String classifier) {
            super(name);
            this.repository = repository;
            this.group = group;
            this.artifact = artifact;
            this.version = version;
            this.classifier = classifier;
        }

        public String getRepository() {
            return repository;
        }

        public String getGroup() {
            return group;
        }

        public String getArtifact() {
            return artifact;
        }

        public String getVersion() {
            return version;
        }

        public String getClassifier() {
            return classifier;
        }

        @Override
        public void writeTo(LittleEndianDataOutput out) throws IOException {
            out.writeByte(MAGIC_NUMBER);
            out.writeByte(ID);
            out.writeString(name);
            out.writeString(repository);
            out.writeString(group);
            out.writeString(artifact);
            out.writeString(version);
            out.writeString(classifier);
        }

        @Override
        public String toString() {
            return String.format("Maven[group=%s, artifact=%s, version=%s, classifier=%s]", group, artifact, version, classifier);
        }
    }
}
