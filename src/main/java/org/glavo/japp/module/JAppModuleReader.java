package org.glavo.japp.module;

import java.io.IOException;
import java.lang.module.ModuleReader;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

public final class JAppModuleReader implements ModuleReader {
    @Override
    public Optional<URI> find(String name) throws IOException {
        return Optional.empty(); // TODO
    }

    @Override
    public Stream<String> list() throws IOException {
        return null; // TODO
    }

    @Override
    public void close() throws IOException {
        // TODO
    }
}
