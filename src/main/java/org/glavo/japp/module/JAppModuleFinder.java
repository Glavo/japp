package org.glavo.japp.module;

import org.glavo.japp.JAppClasspathItem;
import org.glavo.japp.JAppReader;
import org.glavo.japp.JAppResource;

import java.io.IOException;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class JAppModuleFinder implements ModuleFinder {

    private static final String MODULE_INFO = "module-info.class";

    private final JAppReader reader;
    private final Map<String, JAppClasspathItem> items;

    @SuppressWarnings("deprecation")
    private final int release = Runtime.version().major();

    private final Map<String, ModuleReference> cachedModules = new HashMap<>();

    private Set<ModuleReference> all;

    public JAppModuleFinder(JAppReader reader) {
        this.reader = reader;
        this.items = reader.getModulePathItems();
    }

    private ModuleReference load(JAppClasspathItem item) throws IOException {
        JAppResource resource = item.findResource(release, MODULE_INFO);
        if (resource != null) {
            ModuleDescriptor descriptor = ModuleDescriptor.read(reader.getResourceAsInputStream(resource));
            return new JAppModuleReference(descriptor, item);
        } else {
            throw new UnsupportedOperationException("TODO: Automatic Module"); // TODO
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
            all = items.keySet().stream().map(this::find)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
        }

        return all;
    }
}
