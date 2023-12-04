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
package org.glavo.japp.maven;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class MavenResolver {

    public static final MavenRepository CENTRAL = new MavenRepository.Remote("central", "https://repo1.maven.org/maven2");
    public static final MavenRepository LOCAL = new MavenRepository.Local("local", Paths.get("user.home", ".m2", ".repository"));

    private static final Map<String, MavenRepository> repos = new HashMap<>();

    static {
        repos.put(CENTRAL.getName(), CENTRAL);
        repos.put(LOCAL.getName(), LOCAL);
    }

    public static Path resolve(String repoName, String group, String artifact, String version, String classifier) throws Throwable {
        MavenRepository repo;
        if (repoName == null) {
            repo = CENTRAL;
        } else {
            repo = repos.get(repoName);
            if (repo == null) {
                throw new IllegalArgumentException("Unknown repo: " + repoName);
            }
        }

        return repo.resolve(group, artifact, version, classifier);
    }

}
