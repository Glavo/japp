# JApp

A new packaging format for Java programs. 

In the early stages and under active development, stay tuned.

Features being implemented:

* Pack multiple JARs into one file;
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

TODO: Please keep an eye on this project and I will provide a working prototype in the near future.
