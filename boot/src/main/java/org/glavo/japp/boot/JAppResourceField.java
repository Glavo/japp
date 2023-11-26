package org.glavo.japp.boot;

public enum JAppResourceField {
    END,

    /**
     * XXH64 Checksum (8byte)
     */
    CHECKSUM,

    FILE_CREATE_TIME,
    FILE_LAST_MODIFIED_TIME,
    FILE_LAST_ACCESS_TIME
    ;

    private static final JAppResourceField[] FIELDS = values();

    public static JAppResourceField of(int i)  {
        return i >= 0 && i < FIELDS.length ? FIELDS[i] : null;
    }

    public byte id() {
        return (byte) ordinal();
    }
}
