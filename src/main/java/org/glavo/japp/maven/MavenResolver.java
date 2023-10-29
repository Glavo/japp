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

// TODO: Dependencies should be resolved
public final class MavenResolver {

    public static final String CENTRAL = "central";

    private static final Map<String, String> repos = new HashMap<>();

    static {
        repos.put(CENTRAL, "https://repo1.maven.org/maven2");
    }

    public static Path resolve(String repo, String group, String name, String version, String classifier) throws IOException, URISyntaxException {
        if (repo == null) {
            repo = CENTRAL;
        }
        String repoUrl = repos.get(repo);
        if (repoUrl == null) {
            throw new IllegalArgumentException("Unknown repo: " + repo);
        }

        String fileName = name + "-" + version + (classifier == null ? "" : "-" + classifier) + ".jar";
        Path cacheDir = JAppRuntimeContext.getHome()
                .resolve("cache")
                .resolve("maven")
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
        String sha1Url = url + ".sha1";

        String expectedSha1;
        byte[] data;

        URLConnection connection = new URI(sha1Url).toURL().openConnection();
        try (InputStream input = connection.getInputStream()) {
            expectedSha1 = new String(input.readAllBytes()).trim();
            if (expectedSha1.length() != 40) {
                throw new IOException("Invalid SHA-1: " + expectedSha1);
            }
        }

        connection = new URI(url).toURL().openConnection();
        try (InputStream input = connection.getInputStream()) {
            data = input.readAllBytes();
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);

            StringBuilder builder = new StringBuilder(40);
            for (byte b : digest) {
                builder.append(String.format("%02x", b & 0xff));
            }

            String actualSha1 = builder.toString();

            if (!actualSha1.equals(expectedSha1)) {
                throw new IOException("SHA-1 does not match");
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
