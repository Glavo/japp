package org.glavo.japp.launcher;

import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.URLClassPath;
import jdk.internal.module.Modules;
import org.glavo.japp.JAppClasspathItem;
import org.glavo.japp.JAppReader;
import org.glavo.japp.module.JAppModuleFinder;

import java.lang.invoke.MethodHandles;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.*;

public final class BootLauncher {
    private static void addExportsOrOpens(ModuleLayer layer, boolean opens) {
        String prefix = opens
                ? "org.glavo.japp.addopens."
                : "org.glavo.japp.addexports.";

        for (int i = 0; ; i++) {
            String value = System.getProperty(prefix + i);
            if (value == null) {
                return;
            }

            int pos = value.indexOf('=');
            if (pos <= 0) {
                throw new IllegalArgumentException(value);
            }

            String[] left = value.substring(0, pos).split("/");
            if (left.length != 2) {
                throw new IllegalArgumentException(value);
            }

            Module sourceModule = layer.findModule(left[0]).get();
            String sourcePackage = left[1];

            for (String name : value.substring(pos + 1).split(",")) {
                if (name.isEmpty()) {
                    continue;
                }

                Module targetModule = name.equals("ALL-UNNAMED") ? null : layer.findModule(name).get();

                if (opens) {
                    if (targetModule == null) {
                        Modules.addOpensToAllUnnamed(sourceModule, sourcePackage);
                    } else {
                        Modules.addOpens(sourceModule, sourcePackage, targetModule);
                    }
                } else {
                    if (targetModule == null) {
                        Modules.addExportsToAllUnnamed(sourceModule, sourcePackage);
                    } else {
                        Modules.addExports(sourceModule, sourcePackage, targetModule);
                    }
                }
            }
        }

    }

    public static void main(String[] args) throws Throwable {
        JAppReader reader = JAppReader.getSystemReader();

        JAppModuleFinder finder = new JAppModuleFinder(reader);

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(BuiltinClassLoader.class, MethodHandles.lookup());

        BuiltinClassLoader loader = (BuiltinClassLoader) ClassLoader.getSystemClassLoader();
        for (ModuleReference mref : finder.findAll()) {
            loader.loadModule(mref);
        }

        Map<String, JAppClasspathItem> classPath = reader.getClassPathItems();
        if (!classPath.isEmpty()) {
            URLClassPath ucp = (URLClassPath) lookup
                    .findGetter(BuiltinClassLoader.class, "ucp", URLClassPath.class)
                    .invokeExact(loader);

            for (JAppClasspathItem item : classPath.values()) {
                ucp.addURL(item.toURI(false).toURL());
            }
        }

        Configuration configuration = ModuleLayer.boot().configuration()
                .resolve(finder, ModuleFinder.of(), reader.getModulePathItems().keySet());

        ModuleLayer.Controller controller = ModuleLayer.defineModules(configuration, Collections.singletonList(ModuleLayer.boot()), mn -> loader);

        String mainClassName = reader.getMainClass();
        if (mainClassName == null) {
            throw new UnsupportedOperationException("TODO");
        }

        // TODO: Add-Reads

        addExportsOrOpens(controller.layer(), true);
        addExportsOrOpens(controller.layer(), false);

        Class<?> mainClass = Class.forName(mainClassName, false, loader);
        if (mainClass.getModule().isNamed()) {
            controller.addOpens(mainClass.getModule(), mainClass.getPackageName(), BootLauncher.class.getModule());
        }
        mainClass.getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
