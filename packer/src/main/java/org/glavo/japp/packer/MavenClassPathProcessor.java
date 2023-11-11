package org.glavo.japp.packer;

import org.glavo.japp.launcher.JAppResourceReference;
import org.glavo.japp.launcher.maven.MavenResolver;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MavenClassPathProcessor extends ClassPathProcessor {

    private final Pattern pattern = Pattern.compile("(?<group>[^/]+)/(?<artifact>[^/]+)/(?<version>[^/]+)(/(?<classifier>[^/]+))?");

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

        String group = matcher.group("group");
        String artifact = matcher.group("artifact");
        String version = matcher.group("version");
        String classifier = matcher.group("classifier");

        Path file = MavenResolver.resolve(repo, group, artifact, version, classifier);

        if (bundle) {
            LocalClassPathProcessor.addJar(packer, file, isModulePath);
        } else {
            String name;
            if (isModulePath) {
                // TODO
                ModuleFinder finder = ModuleFinder.of(file);
                Set<ModuleReference> all = finder.findAll();
                assert all.size() == 1;
                name = all.iterator().next().descriptor().name();
            } else {
                name = file.getFileName().toString();
            }

            JAppResourceReference.Maven ref = new JAppResourceReference.Maven(name, repo, group, artifact, version, classifier);
            if (isModulePath) {
                packer.current.modulePath.add(ref);
            } else {
                packer.current.classPath.add(ref);
            }
        }
    }
}
