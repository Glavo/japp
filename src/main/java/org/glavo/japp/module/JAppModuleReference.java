package org.glavo.japp.module;

import org.glavo.japp.JAppClasspathItem;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;

public final class JAppModuleReference extends ModuleReference {

    private final JAppClasspathItem item;

    public JAppModuleReference(ModuleDescriptor descriptor, JAppClasspathItem item) {
        super(descriptor, item.toURI(true));
        this.item = item;
    }

    @Override
    public ModuleReader open() throws IOException {


        return null;
    }
}
