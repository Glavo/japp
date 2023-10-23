package org.glavo.japp;

import org.glavo.japp.thirdparty.json.JSONObject;

public abstract class JAppResourceReference {

    public JAppResourceReference fromJson(JSONObject obj) {
        String type = obj.getString("Type");
        if (type.equals(Local.class.getSimpleName())) {
            int index = obj.getInt("Index");
            return new Local(index);
        } else {
            throw new AssertionError("Type: " + type);
        }
    }

    public JSONObject toJson() {
        JSONObject res = new JSONObject();
        if (this instanceof Local) {
            Local local = (Local) this;

            res.put("Type", Local.class.getSimpleName());
            res.put("Index", local.index);
        } else {
            throw new AssertionError("Type: " + this.getClass());
        }

        return res;
    }

    public static final class Local extends JAppResourceReference {
        private final int index;

        public Local(int index) {
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
