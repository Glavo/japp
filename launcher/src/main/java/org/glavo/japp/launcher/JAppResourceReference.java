package org.glavo.japp.launcher;

import org.glavo.japp.json.JSONObject;

public abstract class JAppResourceReference {

    protected JAppResourceReference(String name) {
        this.name = name;
    }

    public static JAppResourceReference fromJson(JSONObject obj) {
        String type = obj.getString("Type");
        String name = obj.optString("Name", null);
        if (type.equals(Local.class.getSimpleName())) {
            int index = obj.getInt("Index");
            return new Local(name, index);
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

    public static final class Local extends JAppResourceReference {
        private final int index;

        public Local(String name, int index) {
            super(name);
            if (index < 0) {
                throw new IndexOutOfBoundsException(Integer.toString(index));
            }

            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return "JAppResourceReference.Local[index=" + index + ']';
        }
    }

    public static final class Maven extends JAppResourceReference {
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
            return "Maven{" +
                   "group='" + group + '\'' +
                   ", artifact='" + artifact + '\'' +
                   ", version='" + version + '\'' +
                   ", classifier='" + classifier + '\'' +
                   '}';
        }
    }
}
