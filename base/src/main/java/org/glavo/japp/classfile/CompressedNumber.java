package org.glavo.japp.classfile;

import java.nio.ByteBuffer;

public final class CompressedNumber {

    private static final int SIGN_MASK = 0b1000_0000;
    private static final int VALUE_MASK = 0b0111_1111;
    private static final int TAIL_VALUE_MASK = 0b0111;

    public static void putInt(ByteBuffer out, int value) {
        if (value < 0) {
            throw new AssertionError();
        }
        do {
            int b = value & VALUE_MASK;
            value >>>= 7;

            if (value != 0) {
                b |= SIGN_MASK;
            }

            out.put((byte) b);
        } while (value != 0);
    }

    public static int getInt(ByteBuffer in) {
        int res = 0;
        for (int i = 0; i < 4; i++) {
            int b = Byte.toUnsignedInt(in.get());
            int bv = b & VALUE_MASK;

            res = res | (bv << (7 * i));
            if (b == bv) {
                return res;
            }
        }

        int b = Byte.toUnsignedInt(in.get());
        int bv = b & VALUE_MASK;
        if (b != bv) {
            throw new AssertionError();
        }

        return res | (bv << (7 * 4));
    }

    private CompressedNumber() {
    }
}
