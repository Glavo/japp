package org.glavo.japp.packer.compressor;

import org.glavo.japp.packer.compressor.classfile.StringPoolBuilder;

public final class CompressContext {
    private final StringPoolBuilder pool = new StringPoolBuilder();

    public StringPoolBuilder getPool() {
        return pool;
    }
}
