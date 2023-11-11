package org.glavo.japp.packer.compressor.classfile;

import org.glavo.japp.TODO;
import org.glavo.japp.packer.compressor.CompressResult;
import org.glavo.japp.packer.compressor.Compressor;

public final class ClassFileCompressor implements Compressor {

    public static final ClassFileCompressor INSTANCE = new ClassFileCompressor();

    @Override
    public CompressResult compress(byte[] source) {
        throw new TODO();
    }
}
