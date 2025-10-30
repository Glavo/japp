import org.glavo.mic.tasks.CompileModuleInfo

plugins {
    id("org.glavo.compile-module-info-plugin") version "2.0" apply false
}

dependencies {
    implementation(project(":base"))
}

tasks.compileJava {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
        )
    )
}

val mainClassName = "org.glavo.japp.boot.JAppBootLauncher"

val compileModuleInfo by tasks.registering(CompileModuleInfo::class) {
    sourceFile.set(layout.projectDirectory.file("src/main/module-info/module-info.java"))
    targetFile.set(layout.buildDirectory.file("classes/module-info/main/module-info.class"))
    moduleMainClass = mainClassName
}

tasks.classes.get().dependsOn(compileModuleInfo)

tasks.register<Jar>("bootJar") {
    destinationDirectory.set(rootProject.layout.buildDirectory)
    archiveFileName.set("japp-boot.jar")

    dependsOn(configurations.runtimeClasspath)

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { zipTree(it) })
}

tasks.withType<Jar> {
    from(compileModuleInfo.map { it.targetFile })
    manifest.attributes("Main-Class" to mainClassName)
}
