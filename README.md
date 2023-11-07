# JApp

A new packaging format for Java programs. 

In the early stages and under active development, stay tuned.

Features implemented:

* Pack multiple modular or non-modular JARs into one file;
* Preserves all `module-info.class`, works well with the JPMS (Java Platform Module System);
* Includes necessary JVM options (e.g. `--add-opens`/`--enable-native-access`) so no need for user to add them;
* Supports adding shebang to the header of the file, so it can be executed as easily as a script;
* Download some dependencies from maven repository (or elsewhere) before running.

Features being implemented:

* Share data between class files to reduce file size;
* Includes some default JVM options that can be easily overridden by the user;
* Automatic selection of applicable Java;
* Supports adding classpath/module/JVM options conditionally;
* Support bundling and loading native libraries;
* Build time optimization.

More details can be found in the draft (in Chinese): [Draft](draft/design.md).

Welcome to discuss in [Discussions](https://github.com/Glavo/japp/discussions).

## Try it

This project is still in its early stages, but we already have a working prototype.

The purpose of the current prototype is for exploration and verification, 
many important features such as compression have not yet been implemented, 
so currently japp files will be much larger than jars.
These are just trade-offs made by the current prototype for ease of debugging, 
and I will gradually address them in the future.

To try this project, you first need to compile it:

```shell
./gradlew
```

(The current prototype only supports Java 9+, it will be compatible with Java 8 in the future.
Compatibility with Java 7 or earlier is not a goal.)

Then, package your program as a japp file:

```shell
./bin/japp-pack.sh --module-path <your-app-module-path> --class-path <your-app-class-path> -o myapp.japp <main-class>
```

Currently supported options:

* `-o <output file>`
* `--module-path <module path>`
* `--class-path <class path>`
* `--add-opens <module>/<package>=<target-module>(,<target-module>)*`
* `--add-exports <module>/<package>=<target-module>(,<target-module>)*`
* `--enable-native-access <module name>[,<module name>...]`
* `-D<name>=<value>`

Now you can run it:

```shell
./myapp.japp <args>
```

## Thanks

Thanks to [PLCT Lab](https://plctlab.github.io/) for supporting me.

<img src="https://resources.jetbrains.com/storage/products/company/brand/logos/IntelliJ_IDEA.svg" alt="IntelliJ IDEA logo.">

This project is developed using JetBrains IDEA. Thanks to JetBrains for providing me with a free license.

