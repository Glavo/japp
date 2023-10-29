package org.glavo.japp.boot.module;

import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.boot.JAppReader;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.TODO;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public final class JAppModuleFinder implements ModuleFinder {

    private static final String MODULE_INFO = "module-info.class";

    private final JAppReader reader;
    private final Map<String, JAppResourceGroup> modules;
    private final ModuleFinder externalModulesFinder;

    private final Map<String, ModuleReference> cachedModules = new HashMap<>();

    private Set<ModuleReference> all;

    public JAppModuleFinder(JAppReader reader, Map<String, JAppResourceGroup> modules, List<Path> externalModules) {
        this.reader = reader;
        this.modules = modules;
        this.externalModulesFinder = externalModules == null ? null : ModuleFinder.of(externalModules.toArray(new Path[0]));
    }

    private static Supplier<Set<String>> packageFinder(JAppResourceGroup group) {
        return () -> {
            Set<String> packages = new HashSet<>();
            for (String name : group.getResources().keySet()) {
                if (name.endsWith(".class") && !name.equals(MODULE_INFO) && !name.startsWith("META-INF/") && name.contains("/")) {
                    int index = name.lastIndexOf("/");
                    if (index != -1) {
                        packages.add(name.substring(0, index).replace('/', '.'));
                    } else {
                        throw new UncheckedIOException(new IOException(name + " in the unnamed package"));
                    }
                }
            }
            return packages;
        };
    }

    private ModuleReference load(JAppResourceGroup group) throws IOException {
        JAppResource resource = group.getResources().get(MODULE_INFO);
        ModuleDescriptor descriptor;
        if (resource != null) {
            descriptor = ModuleDescriptor.read(ByteBuffer.wrap(reader.getResourceAsByteArray(resource)), packageFinder(group));
        } else {
            throw new TODO("Automatic module");
        }
        return new JAppModuleReference(reader, descriptor, group);
    }

    @Override
    public Optional<ModuleReference> find(String name) {
        ModuleReference ref = cachedModules.get(name);
        if (ref != null) {
            return Optional.of(ref);
        }

        JAppResourceGroup group = modules.get(name);
        if (group != null) {
            try {
                ref = load(group);
            } catch (Throwable e) {
                throw new FindException(e);
            }
            cachedModules.put(name, ref);
            return Optional.of(ref);
        }

        if (externalModulesFinder != null) {
            Optional<ModuleReference> res = externalModulesFinder.find(name);
            res.ifPresent(moduleReference -> cachedModules.put(name, moduleReference));
            return res;
        }

        return Optional.empty();
    }

    @Override
    public Set<ModuleReference> findAll() {
        if (all == null) {
            Set<ModuleReference> set = new LinkedHashSet<>();
            modules.forEach((name, item) -> {
                set.add(cachedModules.computeIfAbsent(name, key -> {
                    try {
                        return load(item);
                    } catch (Throwable e) {
                        throw new FindException(e);
                    }
                }));
            });
            if (externalModulesFinder != null) {
                set.addAll(externalModulesFinder.findAll());
            }
            all = Collections.unmodifiableSet(set);
        }

        return all;
    }
}
