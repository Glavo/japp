package org.glavo.japp.compress;

import org.glavo.japp.CompressionMethod;
import org.glavo.japp.TODO;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;

final class DefaultCompressor implements Compressor {

    private final Map<String, CompressionMethod> map = new HashMap<>();

    public DefaultCompressor() {
        String[] none = {
                "png", "apng", "jpg", "jpeg", "webp", "heic", "heif", "avif",
                "aac", "flac", "mp3",
                "mp4", "mkv", "webm",
                "gz", "tgz", "xz", "br", "zst", "bz2", "tbz2"
        };

        for (String ext : none) {
            map.put(ext, CompressionMethod.NONE);
        }
    }


    @Override
    public CompressResult compress(byte[] source, String ext) {
        if (source.length == 0) {
            return new CompressResult(CompressionMethod.NONE, source);
        }

        CompressionMethod method = map.get(ext);
        if (method == null) {
            method = CompressionMethod.DEFLATE;
        }

        if (method == CompressionMethod.NONE) {
            return new CompressResult(method, source);
        }

        if (method == CompressionMethod.DEFLATE) {
            Deflater deflater = new Deflater();
            deflater.setInput(source);
            deflater.finish();

            byte[] res = new byte[source.length];
            int count = 0;

            while (!deflater.finished() && count < res.length) {
                count += deflater.deflate(res, count, res.length - count);
            }

            deflater.end();

            if (count < res.length) {
                return new CompressResult(method, res, 0, count);
            }
        } else {
            throw new TODO("Method: " + method);
        }

        return new CompressResult(method, source);
    }
}
