package org.glavo.japp.boot;

import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.loader.URLClassPath;
import jdk.internal.module.Modules;
import org.glavo.japp.TODO;
import org.glavo.japp.boot.module.JAppModuleFinder;
import org.glavo.japp.json.JSONObject;
import org.glavo.japp.util.IOUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static void addReads(ModuleLayer layer) {
        String prefix = "org.glavo.japp.addreads.";

        for (int i = 0; ; i++) {
            String value = System.getProperty(prefix + i);
            if (value == null) {
                return;
            }

            int pos = value.indexOf('=');
            if (pos <= 0) {
                throw new IllegalArgumentException(value);
            }

            String left = value.substring(0, pos);
            Module sourceModule = layer.findModule(left).get();

            for (String name : value.substring(pos + 1).split(",")) {
                if (name.isEmpty()) {
                    continue;
                }

                Module targetModule = name.equals("ALL-UNNAMED") ? null : layer.findModule(name).get();

                if (targetModule == null) {
                    Modules.addReadsAllUnnamed(sourceModule);
                } else {
                    Modules.addReads(sourceModule, targetModule);
                }
            }
        }
    }

    private static void enableNativeAccess(ModuleLayer layer) {
        String value = System.getProperty("org.glavo.japp.enableNativeAccess");
        if (value == null) {
            return;
        }

        MethodHandle implAddEnableNativeAccess;
        try {
            implAddEnableNativeAccess = MethodHandles.privateLookupIn(Module.class, MethodHandles.lookup())
                    .findVirtual(Module.class, "implAddEnableNativeAccess",
                            MethodType.methodType(Module.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        for (String m : value.split(",")) {
            Module module = layer.findModule(m).get();

            try {
                Module ignored = (Module) implAddEnableNativeAccess.invokeExact(module);
            } catch (Throwable e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static String getNecessaryProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Property " + key + " is not set");
        }
        return value;
    }

    private static Method findMainMethod() throws Throwable {
        @SuppressWarnings("deprecation") int release = Runtime.version().major();
        BuiltinClassLoader loader = (BuiltinClassLoader) ClassLoader.getSystemClassLoader();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(BuiltinClassLoader.class, MethodHandles.lookup());

        String property;

        String file = getNecessaryProperty("org.glavo.japp.file");
        long baseOffset = (property = System.getProperty("org.glavo.japp.file.offset")) != null ? Long.parseLong(property, 16) : 0L;

        long metadataOffset = Long.parseLong(getNecessaryProperty("org.glavo.japp.file.metadataOffset"), 16);

        String modulePaths = System.getProperty("org.glavo.japp.modules");
        String classPaths = System.getProperty("org.glavo.japp.classpath");

        String mainClassName = System.getProperty("org.glavo.japp.mainClass");
        String mainModuleName = System.getProperty("org.glavo.mainModule");

        if (mainClassName == null && mainModuleName == null) {
            throw new IllegalStateException("No main class specified");
        }

        FileChannel channel = FileChannel.open(Paths.get(file));
        channel.position(baseOffset + metadataOffset);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        IOUtils.readFully(channel, buffer);
        int size = buffer.rewind().getInt();
        buffer = ByteBuffer.allocate(size);
        IOUtils.readFully(channel, buffer);
        JAppBootMetadata metadata = JAppBootMetadata.fromJson(new JSONObject(new String(buffer.array(), StandardCharsets.UTF_8)), release);

        Map<String, JAppResourceGroup> modules = new HashMap<>();
        List<Path> externalModules = null;

        Map<String, JAppResourceGroup> classPath = new LinkedHashMap<>();

        if (modulePaths != null) {
            for (String item : modulePaths.split(",")) {
                int idx = item.indexOf(':');

                if (idx <= 0 || idx == item.length() - 1) {
                    throw new IllegalArgumentException("Invalid item: " + item);
                }

                String name = item.substring(0, idx);
                String index = item.substring(idx + 1);

                char ch = index.charAt(0);
                if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f')) {
                    JAppResourceGroup group = metadata.getGroups().get(Integer.parseInt(index, 16));
                    if (!group.getName().equals(name)) {
                        throw new AssertionError(group.getName() + " != " + name);
                    }
                    modules.put(name, group);
                } else if (ch == 'E') {
                    if (externalModules == null) {
                        externalModules = new ArrayList<>();
                    }

                    externalModules.add(Paths.get(index.substring(1)));
                } else {
                    throw new TODO();
                }
            }
        }

        if (classPaths != null) {
            URLClassPath ucp = (URLClassPath) lookup
                    .findGetter(BuiltinClassLoader.class, "ucp", URLClassPath.class)
                    .invokeExact(loader);

            for (String item : classPaths.split(",")) {
                int idx = item.indexOf(':');

                if (idx < 0 || idx == item.length() - 1) {
                    throw new IllegalArgumentException("Invalid item: " + item);
                }

                String name = item.substring(0, idx);
                String index = item.substring(idx + 1);

                char ch = index.charAt(0);
                if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f')) {
                    JAppResourceGroup group = metadata.getGroups().get(Integer.parseInt(index, 16));
                    if (group.getName() == null) {
                        name = "unnamed@" + index;
                        group.name = name;
                    } else if (!name.equals(group.getName())) {
                        throw new AssertionError(group.getName() + " != " + name);
                    }
                    ucp.addURL(JAppResourceRoot.CLASSPATH.toURI(group).toURL());

                    classPath.put(name, group);
                } else if (ch == 'E') {
                    ucp.addURL(Paths.get(index.substring(1)).toUri().toURL());
                } else {
                    throw new TODO();
                }
            }
        }

        JAppReader reader = new JAppReader(channel, baseOffset, modules, classPath);
        JAppReader.initSystemReader(reader);

        ModuleLayer layer;

        if (modules.isEmpty() && externalModules == null) {
            layer = ModuleLayer.boot();
        } else {
            JAppModuleFinder finder = new JAppModuleFinder(reader, modules, externalModules);

            for (ModuleReference mref : finder.findAll()) {
                loader.loadModule(mref);
            }

            Configuration configuration = ModuleLayer.boot().configuration()
                    .resolve(finder, ModuleFinder.of(), reader.getRoot(JAppResourceRoot.MODULES).keySet());

            layer = ModuleLayer.defineModules(configuration, Collections.singletonList(ModuleLayer.boot()), mn -> loader).layer();
        }

        addReads(layer);
        addExportsOrOpens(layer, true);
        addExportsOrOpens(layer, false);
        enableNativeAccess(layer);

        Class<?> mainClass;
        Module mainModule;
        if (mainClassName != null) {
            mainClass = Class.forName(mainClassName, false, loader);
            mainModule = mainClass.getModule();

            if (mainModuleName != null && !mainModule.getName().equals(mainModuleName)) {
                throw new IllegalArgumentException("Class " + mainClassName + " is not in the module " + mainModuleName);
            }
        } else {
            mainModule = layer.findModule(mainModuleName).get();
            mainClassName = mainModule.getDescriptor().mainClass().orElse(null);
            if (mainClassName == null) {
                throw new IllegalArgumentException("Module " + mainModule + " has no main class specified");
            }

            mainClass = Class.forName(mainClassName, false, mainModule.getClassLoader());
        }

        if (mainModule.isNamed()) {
            Modules.addOpens(mainModule, mainClass.getPackageName(), BootLauncher.class.getModule());
        }

        return mainClass.getMethod("main", String[].class);
    }

    public static void main(String[] args) throws Throwable {
        findMainMethod().invoke(null, (Object) args);
    }
}
