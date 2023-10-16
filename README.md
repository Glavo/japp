# JApp

A new packaging format for Java programs. 

In the early stages and under active development, stay tuned.

Features being implemented:

* Pack multiple modular or non-modular JARs into one file;
* Preserves all `module-info.class`, works well with the JPMS (Java Platform Module System);
* Share data between class files to reduce file size;
* Automatic selection of applicable Java;
* Includes necessary JVM options (e.g. `--add-opens`/`--enable-native-access`) so no need for user to add them;
* Includes some default JVM options that can be easily overridden by the user;
* Supports adding shebang to the header of the file, so it can be executed as easily as a script;
* Supports adding classpath/module/JVM options conditionally.

More ideas may be implemented:

* Support bundling and loading native libraries;
* TODO

More details can be found in the draft (in Chinese): [Draft](draft/design.md).

## Try it

This project is still in its early stages, but we already have a working prototype.

The purpose of the current prototype is for exploration and verification, 
many important features such as compression have not yet been implemented, 
so currently japp files will be much larger than jars.
These are just trade-offs made by the current prototype for ease of debugging, 
and I will gradually address them in the future.

To try this project, you first need to compile it:

```shell
./gradlew jar
```

(The current prototype only supports Java 9+, it will be compatible with Java 8 in the future.
Compatibility with Java 7 or earlier is not a goal.)

Then, package your program as a japp file:

```shell
./bin/japp-pack.sh --module-path <your-app-module-path> --classpath <your-app-class-path> -o myapp.japp <main-class>
```

Now you can run it:

```shell
./bin/japp-run.sh myapp.japp <args>
```
