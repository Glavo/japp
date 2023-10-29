package org.glavo.japp.packer;

import org.glavo.japp.TODO;
import org.glavo.japp.maven.MavenResolver;

import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MavenClassPathProcessor extends ClassPathProcessor {

    private final Pattern pattern = Pattern.compile("(?<group>[^/]+)/(?<name>[^/]+)/(?<version>[^/]+)(/(?<classifier>[^/]+))?");

    @Override
    public void process(JAppPacker packer, String path, boolean isModulePath, Map<String, String> options) throws Throwable {
        boolean bundle = !"false".equals(options.remove("bundle"));
        String repo = options.remove("repository");

        if (!options.isEmpty()) {
            throw new IllegalArgumentException("Unrecognized options: " + options.keySet());
        }

        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        Path file = MavenResolver.resolve(repo,
                matcher.group("group"),
                matcher.group("name"),
                matcher.group("version"),
                matcher.group("classifier"));

        if (bundle) {
            LocalClassPathProcessor.addJar(packer, file, isModulePath);
        } else {
            throw new TODO();
        }
    }
}
