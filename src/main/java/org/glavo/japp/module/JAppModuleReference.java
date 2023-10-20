package org.glavo.japp.module;

import org.glavo.japp.JAppClasspathItem;
import org.glavo.japp.JAppReader;
import org.glavo.japp.JAppResource;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

public final class JAppModuleReference extends ModuleReference implements ModuleReader {

    private final JAppReader reader;
    private final JAppClasspathItem item;

    public JAppModuleReference(JAppReader reader, ModuleDescriptor descriptor, JAppClasspathItem item) {
        super(descriptor, item.toURI(true));

        reader.ensureResolved();

        this.reader = reader;
        this.item = item;
    }

    @Override
    public ModuleReader open() throws IOException {
        return this;
    }

    // ModuleReader

    @Override
    public Optional<URI> find(String name) throws IOException {
        JAppResource resource = item.getResources().get(name);
        if (resource != null) {
            return Optional.of(item.toURI(true, resource));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Stream<String> list() throws IOException {
        return item.getResources().values().stream().map(JAppResource::getName);
    }

    @Override
    public void close() throws IOException {
    }
}
