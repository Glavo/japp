package org.glavo.japp.module;

import org.glavo.japp.JAppReader;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Optional;
import java.util.Set;

public final class JAppModuleFinder implements ModuleFinder {

    private final JAppReader file;

    public JAppModuleFinder(JAppReader file) {
        this.file = file;
    }

    @Override
    public Optional<ModuleReference> find(String name) {
        return Optional.empty(); // TODO
    }

    @Override
    public Set<ModuleReference> findAll() {
        return null; // TODO
    }
}
