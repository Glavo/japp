package org.glavo.japp.launcher;

import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.URLClassPath;
import org.glavo.japp.JAppClasspathItem;
import org.glavo.japp.JAppReader;
import org.glavo.japp.module.JAppModuleFinder;

import java.lang.invoke.MethodHandles;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Collections;
import java.util.Map;

public final class BootLauncher {
    public static void main(String[] args) throws Throwable {
        JAppReader reader = JAppReader.getSystemReader();

        @SuppressWarnings("deprecation")
        int release = Runtime.version().major();
        JAppModuleFinder finder = new JAppModuleFinder(reader, release);

        BuiltinClassLoader loader = (BuiltinClassLoader) ClassLoader.getSystemClassLoader();
        for (ModuleReference mref : finder.findAll()) {
            loader.loadModule(mref);
        }

        Map<String, JAppClasspathItem> classPath = reader.getClassPathItems();
        if (!classPath.isEmpty()) {
            URLClassPath ucp = (URLClassPath) MethodHandles.privateLookupIn(BuiltinClassLoader.class, MethodHandles.lookup())
                    .findGetter(BuiltinClassLoader.class, "ucp", URLClassPath.class)
                    .invokeExact(loader);

            for (JAppClasspathItem item : classPath.values()) {
                ucp.addURL(item.toURI(false).toURL());
            }
        }

        Configuration configuration = ModuleLayer.boot().configuration()
                .resolve(finder, ModuleFinder.of(), reader.getModulePathItems().keySet());

        ModuleLayer layer = ModuleLayer.defineModules(configuration, Collections.singletonList(ModuleLayer.boot()), mn -> loader).layer();

        String mainClassName = reader.getMainClass();
        if (mainClassName == null) {
            throw new UnsupportedOperationException("TODO");
        }

        // TODO: Add-Opens and Add-Exports

        Class.forName(mainClassName, false, loader)
                .getMethod("main", String[].class)
                .invoke(null, (Object) args);
    }
}
