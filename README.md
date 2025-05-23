# JApp

[![Gradle Check](https://github.com/Glavo/japp/actions/workflows/check.yml/badge.svg)](https://github.com/Glavo/japp/actions/workflows/check.yml)

JApp is a modern Java program packaging format.

Its design goal is to be a better alternative to shadow jar (fat jar) and launch4j,
and to be the optimal solution for single-file packaging and distribution of Java programs.

The project is under development. We have implemented a prototype, but it still needs to be refined.

Everyone is welcome to report bugs, make feature requests, or discuss designs through [issues](https://github.com/Glavo/japp/issues/new/choose).

## Features and Progress

* Pack multiple modular or non-modular JARs into one file;
  * Unlike Shadow JAR (Fat JAR), JApp has good support for the Java module system;
    Resources from different JARs will be isolated under different prefixes instead of being mixed.

    For example, if we put Gson and Apache Commons Lang 3 as modules into a JApp file,
    their `module-info.class` URIs are as follows:
    
    ```
    japp:/modules/com.google.gson/module-info.class
    japp:/modules/org.apache.commons.lang3/module-info.class
    ```

    JApp also supports using the class path and module path at the same time.
    After adding the above two modules, you can also put Guava into the class path,
    the URI of class `com.google.common.collect.Multimap` is as follows:
    
    ```
    japp:/classpath/guava-32.1.3-jre.jar/com/google/common/collect/Multimap.class
    ```
* JApp files can declare dependencies on JARs from other sources (such as maven repositories).
  The contents of these JARs are not included in the JApp file, but are resolved on demand before running, 
  and then added to the module path or classpath like the JARs within the JApp file.
* Using the [Zstandard](https://github.com/facebook/zstd) compression method, the file size is smaller than JAR;
  * JApp compresses files using the zstd, which decompresses faster and has smaller file sizes than the deflate compression method used by JAR.
    In addition, JApp also compresses file metadata and shares strings in the constant pool of Java Class files,
    so JApp files are usually smaller than JAR files.

    As a test case, I packed the [aya language](https://github.com/aya-prover/aya-dev) as a japp file,
    the original fat jar is 6.81MiB, while the resulting JApp file is only 5.08MiB (-25.40%).
* Automatically select a suitable Java Runtime to start the program based on user-specified conditions;
  * Users can specify some conditions (such as Java version >= 17),
    and then the JApp launcher will find a suitable Java Runtime installed by the user to start the program based on these conditions.
* JApp files can contain JVM options (such as `--add-exports`, `--enable-native-access`, `-D`, etc.), which are passed to the JVM at runtime;
* It supports shebang, so you can run it with just `./myapp.japp <args>`;
* Supports conditional addition of JVM options, classpath, and module paths.

Work in progress:

* More tests;
* Reimplement launcher in native language;
  * The japp launcher's job is to find suitable Java, synthesize JVM options, and class/module paths based on conditions.
    In the current prototype it is implemented in Java, which brings some limitations, I will rewrite it in native language in the future.
* Implement a manager that manages a set of Java;
  * Now that the japp launcher will only scan Java from a fixed list of paths, 
    we need a way to manage the list of available Java runtimes instead.
* Support for filtering unused classes;
* Support for embedding configuration files in JAR;
* Support for Java 8.

To be investigated:

* Support bundling and loading native libraries;
* Supports reading JMod files when creating;
* Proguard support;
* Build time optimization;
* Embed japp file data in the launcher instead of appending it at the end.

## Vision for the future

My vision for this project is to provide two distribution options for all Java projects.

### Distribute JApp files only

In the preferred solution, developers simply package the program into a japp file and distribute it.
This will keep the japp file minimal and easily cross-platform.

Users are expected to install the japp launcher themselves, ideally this should be done via winget/apt/Homebrew etc.
The japp launcher will register as the default program for opening japp files, 
and users should be able to easily launch japp applications from within a file manager or terminal.

In addition, japp launcher can do many other things.
Users can use it to manage Java (similar to sdkman/jenv) and download Java applications (similar to cargo/pip).

### Distributing JApp files with the launcher embedded

This is the second-best option developers can embed the JApp Launcher in a japp file and distribute it.
The final product can have a file extension such as exe/sh.

This will sacrifice the cross-platform capabilities of the program files, but is the most user-friendly, 
as users can launch japp applications without installing Java and the japp launcher themselves.
The embedded japp launcher helps users download Java and prepare the runtime environment.

For cross-platform, we should also try our best to make it not so bad.

For the embedded launcher on Windows platform, we can consider building it for x86 32-bit first, 
making it compatible with Windows x86/x86-64/ARM64 at the same time.

For other platforms, we might be able to develop an embedded launcher in bash that is compatible with macOS,
Linux, BSD, AIX, and even Windows (wsl/msys2/cygwin required).

Furthermore, even if a japp file has an embedded launcher, it should be able to be launched using other launchers.
Developers may consider distributing only a japp file with a launcher embedded for Windows,
and let users of other platforms launch it using a self-installed japp launcher.

## Try it

NOTE: This project is in its early stages.
Some designs have been simplified for convenience, and they will be improved in the future.
The japp file created so far should be used for testing only;
The format of japp files is not yet stable and is subject to change.

To try this project, you first need to build it:

```shell
./gradlew
```

Then, package your program as a japp file:

(For Linux/macOS)
```shell
./bin/japp.sh create -o myapp.japp --module-path <your-app-module-path> --class-path <your-app-class-path> <main-class>
```

(For Windows)
```powershell
.\bin\japp.ps1 create -o myapp.japp --module-path <your-app-module-path> --class-path <your-app-class-path> <main-class>
```

Now you can run it:

(For Linux/macOS)
```shell
./myapp.japp <args>
```

(For Windows)
```powershell
.\bin\japp.ps1 run myapp.japp <args>
```

## Options

The `japp create` command accepts the following basic options:

* `-o <output file>`

### Config Group and Conditions

JApp packages class paths, module paths, JVM options, etc. into **config group**s.
You can add these things to the config group using the following command line options:

* `--module-path <module path>`
* `--class-path <class path>`
* `--add-opens <module>/<package>=<target-module>(,<target-module>)*`
* `--add-exports <module>/<package>=<target-module>(,<target-module>)*`
* `--enable-native-access <module name>[,<module name>...]`
* `-D<name>=<value>`
* `-m <main module>`
* `<main class>`

A config group can have a set of sub-config groups.
By default, these options are added to the root config group.
Use the `--group` command line option to start a new sub-config group, and use the `--end-group` option to end it.

Each config group can specify a **condition** using the `--condition <condition>` option.

Conditions represent requirements for the Java runtime and environment.
For example, condition `java(version: 11, arch: x86-64|aarch64)` indicates 
that the Java runtime version must be at least 11 and the architecture must be x86-64 or AArch64.
You can also combine multiple conditions using `&&` or `||`, such as `java(version: 11) || java(arch: x86-64)`.

The japp launcher will search for a suitable Java runtime based on the condition of the root config group;
If there is no Java runtime that meets the condition, an error will be reported.

The conditions of sub-config groups are used to determine whether the group should be applied.

Example:

```bash
./bin/japp.sh create -o myapp.japp \
  --condition java(version: 11) --module-path ./myapp.jar \
  --group --condition java(version: 22) --enable-native-access=org.glavo.myapp --end-group \
  -m org.glavo.myapp
```

In the above example, assuming that `myapp.jar` exists in the current directory (the java module name is `org.glavo.myapp`),
this command will generate a japp file named `myapp.japp`.
The main module (also the only module in the `myapp.japp`) is `org.glavo.myapp`.

All you need to run it is this:

```bash
./myapp.japp
```

The japp launcher looks for a Java runtime version 11 or higher to run the program.
If the Java runtime found is of version 22 or higher, the JVM option `--enable-native-access=org.glavo.myapp` is added.

### Classpath and Module Path

The `--module-path` and `--classpath` options above accept arguments similar to the `java`/`javac` command:

(For Windows, please replace the path separator with `;`)
```
--module-path <path 1>:...:<path n>
```

For the `java`/`javac` command, each path must be a file.
But for japp, each path can contain an optional prefix `[<key1>=<value1>,...,<key n>=<value n>]` to specify options.
We can use this syntax to specify to look for jars from the maven repository instead of locally:

```
[type=maven]<group>/<artifact>/<version>
```

For example, the following option will add gson to the module path:

```
--module-path [type=maven]com.google.code.gson/gson/2.10
```

By default, this dependency is bundled into the japp file just like a normal module path item.
However, you can use the `bundle=false` option to tell japp to only declare a dependency on it and not bundle its contents into the japp file:

```
--module-path [type=maven,bundle=false]com.google.code.gson/gson/2.10
```

When running this japp file, the japp launcher will first download the dependencies locally,
then add it to the module path and then start the program.

## Thanks

<img alt="PLCT Logo" src="./PLCT.svg" width="200" height="200">

Thanks to [PLCT Lab](https://plctlab.github.io/) for supporting me.

<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA.svg" alt="IntelliJ IDEA logo.">

This project is developed using JetBrains IDEA. Thanks to JetBrains for providing me with a free license.
