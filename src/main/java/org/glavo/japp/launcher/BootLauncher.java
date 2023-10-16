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
import java.util.Collections;
import java.util.Map;

public final class BootLauncher {
    private static void addExportsOrOpens(ModuleLayer layer, String item, boolean open) {
        int idx0 = item.indexOf('/');

        if (idx0 <= 0) {
            throw new IllegalArgumentException(item);
        }

        Module source = layer.findModule(item.substring(0, idx0)).get();

        int idx1 = item.indexOf('=', idx0 + 1);
        if (idx1 <= idx0 + 1 || idx1 == item.length() - 1) {
            throw new IllegalArgumentException(item);
        }

        String packageName = item.substring(idx0 + 1, idx1);

        String[] targets = item.substring(idx1 + 1).split(",");

        for (String targetName : targets) {
            Module target = targetName.equals("ALL-UNNAMED") ? null : layer.findModule(targetName).get();

            if (open) {
                if (target == null) {
                    Modules.addOpensToAllUnnamed(source, packageName);
                } else {
                    Modules.addOpens(source, packageName, target);
                }
            } else {
                if (target == null) {
                    Modules.addExportsToAllUnnamed(source, packageName);
                } else {
                    Modules.addExports(source, packageName, target);
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        JAppReader reader = JAppReader.getSystemReader();

        @SuppressWarnings("deprecation")
        int release = Runtime.version().major();
        JAppModuleFinder finder = new JAppModuleFinder(reader, release);

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

        for (String item : reader.getAddOpens()) {
            addExportsOrOpens(controller.layer(), item, true);
        }

        for (String item : reader.getAddExports()) {
            addExportsOrOpens(controller.layer(), item, false);
        }

        Class<?> mainClass = Class.forName(mainClassName, false, loader);
        if (mainClass.getModule().isNamed()) {
            controller.addOpens(mainClass.getModule(), mainClass.getPackageName(), BootLauncher.class.getModule());
        }
        mainClass.getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
