package org.glavo.japp.launcher;

import org.glavo.japp.thirdparty.json.JSONObject;

public abstract class JAppResourceReference {

    protected JAppResourceReference(String name) {
        this.name = name;
    }

    public static JAppResourceReference fromJson(JSONObject obj) {
        String type = obj.getString("Type");
        String name = obj.optString("Name");
        if (type.equals(Local.class.getSimpleName())) {
            int index = obj.getInt("Index");
            return new Local(name, index);
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
        if (this instanceof Local) {
            Local local = (Local) this;
            res.put("Type", Local.class.getSimpleName());
            res.putOpt("Name", local.name);
            res.put("Index", local.index);
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
}
