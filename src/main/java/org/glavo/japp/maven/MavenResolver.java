package org.glavo.japp.maven;

import org.glavo.japp.JAppRuntimeContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public final class MavenResolver {

    public static final String CENTRAL = "central";

    private static final Map<String, String> repos = new HashMap<>();

    static {
        repos.put(CENTRAL, "https://repo1.maven.org/maven2");
    }

    public static Path resolve(String repo, String group, String name, String version, String classifier) throws IOException, URISyntaxException {
        String repoUrl = repos.get(repo);
        if (repoUrl == null) {
            throw new IllegalArgumentException("Unknown repo: " + repo);
        }

        String fileName = name + "-" + version + (classifier == null ? "" : "-" + classifier) + ".jar";
        Path cacheDir = JAppRuntimeContext.getHome()
                .resolve("cache")
                .resolve(repo)
                .resolve(group)
                .resolve(name)
                .resolve(version);

        Files.createDirectories(cacheDir);

        Path file = cacheDir.resolve(fileName);
        if (Files.isRegularFile(file)) {
            return file;
        }

        String url = repoUrl + "/" + group.replace('.', '/') + "/" + name + "/" + version + "/" + fileName;
        String sha256Url = url + ".sha256";

        String expectedSha256;
        byte[] data;

        URLConnection connection = new URI(sha256Url).toURL().openConnection();
        try (InputStream input = connection.getInputStream()) {
            expectedSha256 = new String(input.readAllBytes()).trim();
            if (expectedSha256.length() != 64) {
                throw new IOException("Invalid SHA-256: " + expectedSha256);
            }
        }

        connection = new URI(url).toURL().openConnection();
        try (InputStream input = connection.getInputStream()) {
            data = input.readAllBytes();
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);

            StringBuilder builder = new StringBuilder(64);
            for (byte b : digest) {
                builder.append(String.format("%02x", b & 0xff));
            }

            String actualSha256 = builder.toString();

            if (!actualSha256.equals(expectedSha256)) {
                throw new IOException("SHA-256 does not match");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }

        Path temp = cacheDir.resolve(fileName + ".temp");
        Files.write(temp, data);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        return file;
    }


}
