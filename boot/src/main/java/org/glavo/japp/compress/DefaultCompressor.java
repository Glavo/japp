package org.glavo.japp.compress;

import java.util.HashMap;
import java.util.Map;

final class DefaultCompressor implements Compressor {

    private final Map<String, CompressionMethod> map = new HashMap<>();

    public DefaultCompressor() {
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
    public CompressResult compress(byte[] source, String ext) {
        if (source.length <= 24) {
            return new CompressResult(CompressionMethod.NONE, source);
        }

        CompressionMethod method = map.get(ext);
        if (method == null) {
            method = CompressionMethod.DEFLATE;
        }

        CompressResult result = method.compress(source);
        if (result.getLength() < source.length) {
            return result;
        }
        return new CompressResult(CompressionMethod.NONE, source);
    }
}
