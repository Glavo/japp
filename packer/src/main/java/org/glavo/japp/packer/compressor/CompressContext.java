package org.glavo.japp.packer.compressor;

import org.glavo.japp.packer.compressor.classfile.ByteArrayPoolBuilder;

public final class CompressContext {
    private final ByteArrayPoolBuilder pool = new ByteArrayPoolBuilder();

    public ByteArrayPoolBuilder getPool() {
        return pool;
    }
}
