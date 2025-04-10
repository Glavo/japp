Hi everyone, I'm working on a more modern packaging format for Java programs to replace shadow(fat) jars.

I already have a prototype and hope to get more people to discuss the design before moving on to the next step.

Here are some features:

* Pack multiple modular or non-modular JARs into one file.
* Full support for JPMS (Java Module System).
* Smaller file size (via zstd, metadata compression, and constant pool sharing).
* Supports declaring some requirements for Java (such as Java version) and looking for a Java that meets the requirements at startup.
* Supports declaring some JVM options (such as `--add-exports`, `--enable-native-access`, `-Da=b`, etc.)
* Support for declaring external dependencies. Artifacts from Maven Central can be added to the classpath or module path to be downloaded on demand at startup.
* Supports conditional addition of JVM options, classpath, and module paths.
* Supports appending other content to the file header. For example, we can append an exe/bash launcher to the header to download Java and launch the program.

My ambition is to make it the standard way of distributing Java programs.

The official strong recommendation of jlink makes me feel uneasy.

Think about it, in a world dominated by jlink and jpackage, who cares about users of niche platforms like FreeBSD, AIX, and Linux RISC-V 64?

Although jlink can package program for other platforms, it is different from Golang, 
which can easily generate small executable files for different platforms.
The jlink needs to download a JDK for each target platform,
and the packaged program ranges from tens of megabytes to hundreds of megabytes.

For each additional target platform,
an additional 200MB of JDK must be downloaded to the packaging device,
the packaging time will increase by tens of seconds to several minutes,
and finally an additional file of tens to hundreds of MB must be distributed.
This reduces the incentive for developers to provide distribution to more platforms.

Another thing to consider is, where do we download these JDKs from?
Most developers choose a vendor they trust (such as Eclipse Adoptium, BellSoft, and Azul)
and download all JDKs from him.
This means that the compatibility of the programs they distribute often depends on this vendor.

The platforms supported by these vendors cover most of the common platforms,
but there are some niche platforms that are not taken care of.
For example, platforms like FreeBSD, Alpine Linux, and Linux LoongArch 64, few JDK vendors considers them,
and Java on these platforms is often provided by the package manager.
Therefore, these platforms are rarely considered by developers who use jlink to package programs.

I want to be able to download a single program file of a few hundred KB to a few MB, rather than a compressed package of a few hundred MB.

I want to avoid having dozens of JVMs and Java standard libraries on disk.

I want to be able to easily get programs that work on Linux RISC-V 64 or FreeBSD.

If you have these wishes like me, I hope to get your help.