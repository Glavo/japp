# What's wrong with the existing packaging format?

For Java programs, we now have many ways to package them, each with different advantages and disadvantages:

## jlink/jpackage

This is the packaging method currently promoted by OpenJDK officials.

jlink trims the JDK to retain only the required modules, and then packages the JDK and the program together.

Advantage:

* The program comes with a Java runtime environment, so users do not need to install additional dependencies;
* Developers can choose the Java runtime to use, avoiding compatibility issues as much as possible.

Disadvantage:

* jlink compromises the cross-platform capabilities of Java programs.
  This makes it more difficult for users other than Windows/Linux/macOS systems and x86/ARM architectures
  (such as Linux RISC-V/LoongArch and AIX users) to use Java programs.

  Most Java programs are actually perfectly cross-platform and can run on any platform that supports the JVM.
  However, jlink packages the program with the native files of a specific platform,
  so that the packaged program can only run on one platform.

  Although jlink supports cross-building and can package program for other platforms.
  However, this is different from Golang, which can easily generate small executable files for different platforms.
  jlink needs to download a JDK for each target platform,
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
  and Java on these platforms is often provided by the system's package manager.
  Therefore, these platforms are rarely considered by developers who use jlink to package programs.
* Since the program is always distributed with the Java runtime environment,
  this greatly increases the size of the programs
  and the Java runtime can no longer be shared between multiple Java programs.

  For end users, the countless chromium on the disk has already troubled many people,
  and jlink has brings countless Java standard libraries/JVMs to their disks.

  For servers, the Java runtime environment does not need to change often.
  Traditionally, the Java runtime environment can be installed on the system, or in a base image.
  If you want to use jlink, you have to transfer the entire Java standard library/JVM for every update,
  which is a complete waste.
* Currently, jlink packages a zip archive instead of a single executable file.
  Users need to find a place to unzip/install the program before they can use it,
  which is not so convenient and flexible.

  The [Hermetic Java](https://cr.openjdk.org/~jiangli/hermetic_java.pdf) project plans to address this issue,
  but it is unknown when it will be completed.

## Shadow(Fat) JAR

Shadow(Fat) JAR technology packages the class files and resources of a Java program and all its dependencies into a single JAR file.

Advantage:

* Single file, small, cross-platform, and fast to package.

Disadvantage:

* Users need to install Java to run it;
* It cannot choose which Java to start itself with;
* It is not possible to pass JVM options to the JVM and only provides limited control over the JVM through the manifest file;
* Since a JAR file can contain only one module, it does not work well with JPMS and often only works with the classpath.