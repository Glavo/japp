package org.glavo.japp.boot.module;

import org.glavo.japp.boot.JAppResourceGroup;
import org.glavo.japp.boot.JAppReader;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.boot.JAppResourceRoot;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

public final class JAppModuleReference extends ModuleReference implements ModuleReader {

    private final JAppReader reader;
    private final JAppResourceGroup group;

    public JAppModuleReference(JAppReader reader, ModuleDescriptor descriptor, JAppResourceGroup group) {
        super(descriptor, JAppResourceRoot.MODULES.toURI(group));

        this.reader = reader;
        this.group = group;
    }

    @Override
    public ModuleReader open() throws IOException {
        return this;
    }

    // ModuleReader

    @Override
    public Optional<URI> find(String name) throws IOException {
        JAppResource resource = group.getResources().get(name);
        if (resource != null) {
            return Optional.of(JAppResourceRoot.MODULES.toURI(group, resource));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Stream<String> list() throws IOException {
        return group.getResources().values().stream().map(JAppResource::getName);
    }

    @Override
    public void close() throws IOException {
    }
}
