package org.glavo.japp.compress.lz4;

final class Match {
    int start, ref, len;

    void fix(int correction) {
        start += correction;
        ref += correction;
        len -= correction;
    }

    int end() {
        return start + len;
    }

    void copyTo(Match target) {
        target.len = len;
        target.start = start;
        target.ref = ref;
    }
}
