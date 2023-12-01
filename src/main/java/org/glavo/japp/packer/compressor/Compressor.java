package org.glavo.japp.packer.compressor;

import org.glavo.japp.packer.JAppPacker;

import java.io.IOException;

@FunctionalInterface
public interface Compressor {

    CompressResult compress(JAppPacker packer, byte[] source) throws IOException;

    default CompressResult compress(JAppPacker packer, byte[] source, String filePath) throws IOException {
        return compress(packer, source);
    }
}
