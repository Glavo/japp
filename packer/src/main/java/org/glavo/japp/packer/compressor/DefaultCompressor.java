package org.glavo.japp.packer.compressor;

import org.glavo.japp.TODO;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.packer.JAppPacker;
import org.glavo.japp.packer.compressor.classfile.ClassFileCompressor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class DefaultCompressor implements Compressor {

    private final Map<String, CompressionMethod> map = new HashMap<>();

    public DefaultCompressor() {
        // map.put("class", CompressionMethod.CLASSFILE);

        for (String ext : new String[]{
                "png", "apng", "jpg", "jpeg", "webp", "heic", "heif", "avif",
                "aac", "flac", "mp3",
                "mp4", "mkv", "webm",
                "gz", "tgz", "xz", "br", "zst", "bz2", "tbz2"
        }) {
            map.put(ext, CompressionMethod.NONE);
        }
    }

    @Override
    public CompressResult compress(JAppPacker packer, byte[] source) throws IOException {
        return compress(packer, source, (String) null);
    }

    @Override
    public CompressResult compress(JAppPacker packer, byte[] source, String ext) throws IOException {
        if (source.length <= 16) {
            return new CompressResult(source);
        }

        CompressionMethod method = map.getOrDefault(ext, CompressionMethod.DEFLATE);
        CompressResult result;
        switch (method) {
            case NONE:
                result = new CompressResult(source);
                break;
            case CLASSFILE:
                try {
                    result = ClassFileCompressor.INSTANCE.compress(packer, source);
                } catch (Throwable e) {
                    // Malformed class file

                    // TODO: Test ClassFileCompressor
                    e.printStackTrace();
                    result = Compressor.DEFLATE.compress(packer, source);
                }
                break;
            case DEFLATE:
                result = Compressor.DEFLATE.compress(packer, source);
                break;
            case LZ4:
                result = Compressor.LZ4.compress(packer, source);
                break;
            default:
                throw new TODO("Method: " + method);
        }

        if (result.getLength() < source.length) {
            return result;
        }
        return new CompressResult(source);
    }
}
