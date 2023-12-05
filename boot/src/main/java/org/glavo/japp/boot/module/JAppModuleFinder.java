/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.boot.module;

import org.glavo.japp.boot.JAppReader;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.boot.JAppResourceGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.*;

public final class JAppModuleFinder implements ModuleFinder {

    private static final String MODULE_INFO = "module-info.class";
    private static final String SERVICES_PREFIX = "META-INF/services/";

    private final JAppReader reader;
    private final Map<String, JAppResourceGroup> modules;
    private final ModuleFinder externalModulesFinder;

    private Map<String, ModuleReference> all;

    public JAppModuleFinder(JAppReader reader, Map<String, JAppResourceGroup> modules, List<Path> externalModules) {
        this.reader = reader;
        this.modules = modules;
        this.externalModulesFinder = externalModules == null ? null : ModuleFinder.of(externalModules.toArray(new Path[0]));
    }

    private static Set<String> findPackages(JAppResourceGroup group) {
        Set<String> packages = new HashSet<>();
        for (String name : group.keySet()) {
            if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                int index = name.lastIndexOf("/");
                if (index >= 0) {
                    packages.add(name.substring(0, index).replace('/', '.'));
                }
            }
        }
        return packages;
    }

    private ModuleDescriptor deriveModuleDescriptor(JAppResourceGroup group) throws IOException {
        ModuleDescriptor.Builder builder = ModuleDescriptor.newAutomaticModule(group.getName());

        Set<String> packages = new HashSet<>();

        for (String name : group.keySet()) {
            if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                int index = name.lastIndexOf("/");
                if (index >= 0) {
                    packages.add(name.substring(0, index).replace('/', '.'));
                }
            } else if (name.startsWith(SERVICES_PREFIX)) {
                String sn = name.substring(SERVICES_PREFIX.length());
                if (sn.contains("/")) {
                    continue;
                }

                List<String> providerClasses = new ArrayList<>();
                try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(reader.openResource(group.get(name))))) {
                    String line;
                    while ((line = fileReader.readLine()) != null) {
                        if (!line.isEmpty()) {
                            providerClasses.add(line);
                        }
                    }
                }

                if (!providerClasses.isEmpty())
                    builder.provides(sn, providerClasses);
            }
        }

        builder.packages(packages);

        return builder.build();
    }

    private ModuleReference load(JAppResourceGroup group) throws IOException {
        JAppResource resource = group.get(MODULE_INFO);
        ModuleDescriptor descriptor;
        if (resource != null) {
            descriptor = ModuleDescriptor.read(reader.readResource(resource), () -> findPackages(group));
        } else {
            descriptor = deriveModuleDescriptor(group);
        }
        return new JAppModuleReference(reader, descriptor, group);
    }

    private void loadAll() {
        if (all != null) {
            return;
        }

        Map<String, ModuleReference> map = new HashMap<>();

        for (Map.Entry<String, JAppResourceGroup> entry : modules.entrySet()) {
            String name = entry.getKey();
            JAppResourceGroup group = entry.getValue();
            ModuleReference ref;
            try {
                ref = load(group);
            } catch (Throwable e) {
                throw new FindException(e);
            }

            if (map.put(name, ref) != null) {
                throw new FindException("Duplicate module " + name);
            }
        }

        if (externalModulesFinder != null) {
            Set<ModuleReference> allExternal = externalModulesFinder.findAll();
            for (ModuleReference external : allExternal) {
                String name = external.descriptor().name();
                if (map.put(name, external) != null) {
                    throw new FindException("Duplicate module " + name);
                }
            }
        }

        all = map;
    }

    @Override
    public Optional<ModuleReference> find(String name) {
        loadAll();
        return Optional.ofNullable(all.get(name));
    }

    @Override
    public Set<ModuleReference> findAll() {
        loadAll();
        return new HashSet<>(all.values());
    }
}
