package org.glavo.japp.module;

import org.glavo.japp.JAppClasspathItem;
import org.glavo.japp.JAppReader;
import org.glavo.japp.JAppResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;

public final class JAppModuleFinder implements ModuleFinder {

    private static final String MODULE_INFO = "module-info.class";

    private final JAppReader reader;
    private final Map<String, JAppClasspathItem> items;

    private final int release;

    private final Map<String, ModuleReference> cachedModules = new HashMap<>();

    private Set<ModuleReference> all;

    public JAppModuleFinder(JAppReader reader, int release) {
        this.reader = reader;
        this.items = reader.getModulePathItems();
        this.release = release;
    }

    private ModuleReference load(JAppClasspathItem item) throws IOException {
        JAppResource resource = item.findResource(release, MODULE_INFO);
        if (resource != null) {
            Supplier<Set<String>> packageFinder = () -> {
                Set<String> packages = new HashSet<>();
                findAllPackage(packages, item.getResources().keySet());

                for (int i = 9; i <= release; i++) {
                    Map<String, JAppResource> multiReleaseResources = item.getMultiRelease(i);
                    if (multiReleaseResources != null) {
                        findAllPackage(packages, multiReleaseResources.keySet());
                    }
                }

                return packages;
            };


            ModuleDescriptor descriptor = ModuleDescriptor.read(reader.getResourceAsInputStream(resource), packageFinder);
            return new JAppModuleReference(reader, descriptor, item, release);
        } else {
            throw new UnsupportedOperationException("TODO: Automatic Module"); // TODO
        }
    }

    private static void findAllPackage(Set<String> packages, Collection<String> resources) {
        for (String name : resources) {
            if (name.endsWith(".class") && !name.equals(MODULE_INFO) && !name.startsWith("META-INF/") && name.contains("/")) {
                int index = name.lastIndexOf("/");
                if (index != -1) {
                    packages.add(name.substring(0, index).replace('/', '.'));
                } else {
                    throw new UncheckedIOException(new IOException(name  + " in the unnamed package"));
                }
            }
        }
    }

    @Override
    public Optional<ModuleReference> find(String name) {
        ModuleReference ref = cachedModules.get(name);
        if (ref != null) {
            return Optional.of(ref);
        }

        JAppClasspathItem item = items.get(name);
        if (item == null) {
            return Optional.empty();
        }

        try {
            ref = load(item);
        } catch (Throwable e) {
            throw new FindException(e);
        }
        cachedModules.put(name, ref);
        return Optional.of(ref);
    }

    @Override
    public Set<ModuleReference> findAll() {
        if (all == null) {
            Set<ModuleReference> set = new LinkedHashSet<>();
            items.forEach((name, item) -> {
                set.add(cachedModules.computeIfAbsent(name, key -> {
                    try {
                        return load(item);
                    } catch (Throwable e) {
                        throw new FindException(e);
                    }
                }));
            });
            all = Collections.unmodifiableSet(set);
        }

        return all;
    }
}
