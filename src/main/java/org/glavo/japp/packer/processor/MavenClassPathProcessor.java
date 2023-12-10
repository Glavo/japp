/*
 * Copyright (C) 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.packer.processor;

import org.glavo.japp.launcher.JAppResourceGroupReference;
import org.glavo.japp.maven.MavenResolver;
import org.glavo.japp.packer.JAppWriter;

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
    public void process(JAppWriter packer, String path, boolean isModulePath, Map<String, String> options) throws Throwable {
        boolean bundle = !"false".equals(options.remove("bundle"));
        String repo = options.remove("repository");
        boolean verify = !"false".equals(options.remove("verify")); // TODO

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
                ModuleFinder finder = ModuleFinder.of(file); // TODO: need opt
                Set<ModuleReference> all = finder.findAll();
                assert all.size() == 1;
                name = all.iterator().next().descriptor().name();
            } else {
                name = file.getFileName().toString();
            }

            packer.addReference(
                    new JAppResourceGroupReference.Maven(name, repo, group, artifact, version, classifier),
                    isModulePath
            );
        }
    }
}
