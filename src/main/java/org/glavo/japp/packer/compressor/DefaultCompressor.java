package org.glavo.japp.packer.compressor;

import org.glavo.japp.TODO;
import org.glavo.japp.CompressionMethod;
import org.glavo.japp.packer.JAppPacker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class DefaultCompressor implements Compressor {

    private final Map<String, CompressionMethod> map = new HashMap<>();
    private final CompressionMethod defaultMethod = CompressionMethod.ZSTD;

    public DefaultCompressor() {
        map.put("class", CompressionMethod.CLASSFILE);

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
        return compress(packer, source, null);
    }

    @Override
    public CompressResult compress(JAppPacker packer, byte[] source, String filePath) throws IOException {
        if (source.length <= 16) {
            return new CompressResult(source);
        }

        String ext;
        if (filePath != null) {
            int sep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
            int dot = filePath.lastIndexOf('.');
            ext = dot > sep ? filePath.substring(dot + 1) : null;
        } else {
            ext = null;
        }

        CompressionMethod method = map.getOrDefault(ext, defaultMethod);
        CompressResult result;
        switch (method) {
            case NONE:
                result = new CompressResult(source);
                break;
            case CLASSFILE:
                try {
                    result = Compressors.CLASSFILE.compress(packer, source);
                } catch (Throwable e) {
                    // Malformed class file

                    // TODO: Test ClassFileCompressor
                    e.printStackTrace();
                    result = Compressors.ZSTD.compress(packer, source);
                }
                break;
            case ZSTD:
                result = Compressors.ZSTD.compress(packer, source);
                break;
            default:
                throw new TODO("Method: " + method);
        }

        return result.getLength() < source.length ? result : new CompressResult(source);
    }
}
