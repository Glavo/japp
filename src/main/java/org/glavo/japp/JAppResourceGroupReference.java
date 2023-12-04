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

import org.glavo.japp.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

public abstract class JAppResourceGroupReference {

    protected JAppResourceGroupReference(String name) {
        this.name = name;
    }

    public static JAppResourceGroupReference fromJson(JSONObject obj) {
        String type = obj.getString("Type");
        String name = obj.optString("Name", null);
        if (type.equals(Local.class.getSimpleName())) {
            int index = obj.getInt("Index");
            JSONObject multiRelease = obj.optJSONObject("Multi-Release");
            TreeMap<Integer, Integer> multiReleaseIndexes;

            if (multiRelease != null) {
                multiReleaseIndexes = new TreeMap<>();
                for (String key : multiRelease.keySet()) {
                    multiReleaseIndexes.put(Integer.parseInt(key), multiRelease.getInt(key));
                }

            } else {
                multiReleaseIndexes = null;
            }

            return new Local(name, index, multiReleaseIndexes);
        } else if (type.equals(Maven.class.getSimpleName())) {
            String repository = obj.optString("Repository", null);
            String group = obj.getString("Group");
            String artifact = obj.getString("Artifact");
            String version = obj.getString("Version");
            String classifier = obj.optString("Classifier", null);

            return new Maven(name, repository, group, artifact, version, classifier);
        } else {
            throw new AssertionError("Type: " + type);
        }
    }

    public final String name;

    public String getName() {
        return name;
    }

    public JSONObject toJson() {
        JSONObject res = new JSONObject();
        res.put("Type", getClass().getSimpleName());
        if (this instanceof Local) {
            Local local = (Local) this;
            res.putOpt("Name", local.name);
            res.put("Index", local.index);

            if (local.getMultiReleaseIndexes() != null) {
                JSONObject multiRelease = new JSONObject();

                for (Map.Entry<Integer, Integer> entry : local.getMultiReleaseIndexes().entrySet()) {
                    multiRelease.put(String.valueOf(entry.getKey()), entry.getValue());
                }

                res.put("Multi-Release", multiRelease);
            }

        } else if (this instanceof Maven) {
            Maven maven = (Maven) this;
            res.putOpt("Repository", maven.getRepository());
            res.put("Group", maven.getGroup());
            res.put("Artifact", maven.getArtifact());
            res.put("Version", maven.getVersion());
            res.putOpt("Classifier", maven.getClassifier());
        } else {
            throw new AssertionError("Type: " + this.getClass());
        }

        return res;
    }

    public static final class Local extends JAppResourceGroupReference {
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
        public String toString() {
            if (multiReleaseIndexes == null) {
                return "Local[index=" + index + ']';
            } else {
                return String.format("Local[index=%d, multiReleaseIndexes=%s]", index, multiReleaseIndexes);
            }
        }
    }

    public static final class Maven extends JAppResourceGroupReference {
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
        public String toString() {
            return String.format("Maven[group=%s, artifact=%s, version=%s, classifier=%s]", group, artifact, version, classifier);
        }
    }
}
