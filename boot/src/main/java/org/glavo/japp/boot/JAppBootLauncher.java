/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.boot;

import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.module.Modules;
import org.glavo.japp.boot.module.JAppModuleFinder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public final class JAppBootLauncher {
    private static void addExportsOrOpens(ModuleLayer layer, boolean opens, List<String> list) {
        for (String value : list) {
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

    private static void addReads(ModuleLayer layer, List<String> list) {
        for (String value : list) {
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

    private static void enableNativeAccess(ModuleLayer layer, List<String> list) {
        MethodHandle implAddEnableNativeAccess;
        try {
            implAddEnableNativeAccess = MethodHandles.privateLookupIn(Module.class, MethodHandles.lookup())
                    .findVirtual(Module.class, "implAddEnableNativeAccess",
                            MethodType.methodType(Module.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        for (String m : list) {
            Optional<Module> module = layer.findModule(m);
            if (!module.isPresent()) {
                throw new IllegalArgumentException("Module " + m + " not found");
            }

            try {
                Module ignored = (Module) implAddEnableNativeAccess.invokeExact(module.get());
            } catch (Throwable e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private static Method findMainMethod() throws Throwable {
        String bootArgs = System.getProperty("org.glavo.japp.boot.args");
        if (bootArgs == null) {
            throw new Error("Miss boot args");
        }

        JAppBootArgs args = JAppReader.openSystemReader(ByteBuffer.wrap(Base64.getDecoder().decode(bootArgs)).order(ByteOrder.LITTLE_ENDIAN));
        if (args.mainClass == null && args.mainModule == null) {
            throw new IllegalStateException("No main class specified");
        }

        JAppReader reader = JAppReader.getSystemReader();
        BuiltinClassLoader loader = (BuiltinClassLoader) ClassLoader.getSystemClassLoader();
        ModuleLayer layer;

        Map<String, JAppResourceGroup> modules = reader.getRoot(JAppResourceRoot.MODULES);
        if (modules.isEmpty() && args.externalModules.isEmpty()) {
            layer = ModuleLayer.boot();
        } else {
            JAppModuleFinder finder = new JAppModuleFinder(reader, modules, args.externalModules);
            Set<ModuleReference> references = finder.findAll();
            Set<String> allModuleNames = new HashSet<>();

            for (ModuleReference mref : references) {
                loader.loadModule(mref);
                allModuleNames.add(mref.descriptor().name());
            }

            Configuration configuration = ModuleLayer.boot().configuration()
                    .resolve(finder, ModuleFinder.of(), allModuleNames);

            layer = ModuleLayer.defineModules(configuration, Collections.singletonList(ModuleLayer.boot()), mn -> loader).layer();
        }

        addReads(layer, args.addReads);
        addExportsOrOpens(layer, true, args.addOpens);
        addExportsOrOpens(layer, false, args.addExports);
        enableNativeAccess(layer, args.enableNativeAccess);

        Class<?> mainClass;
        Module mainModule;
        if (args.mainClass != null) {
            mainClass = Class.forName(args.mainClass, false, loader);
            mainModule = mainClass.getModule();

            if (args.mainModule != null && !mainModule.getName().equals(args.mainModule)) {
                throw new IllegalArgumentException("Class " + mainClass + " is not in the module " + args.mainModule);
            }
        } else {
            mainModule = layer.findModule(args.mainModule).orElseThrow(IllegalArgumentException::new);
            String mainClassName = mainModule.getDescriptor().mainClass().orElse(null);
            if (mainClassName == null) {
                throw new IllegalArgumentException("Module " + mainModule + " has no main class specified");
            }

            mainClass = Class.forName(mainClassName, false, mainModule.getClassLoader());
        }

        if (mainModule.isNamed()) {
            Modules.addOpens(mainModule, mainClass.getPackageName(), JAppBootLauncher.class.getModule());
        }

        return mainClass.getMethod("main", String[].class);
    }

    public static void main(String[] args) throws Throwable {
        try {
            findMainMethod().invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
