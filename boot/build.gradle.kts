plugins {
    id("org.glavo.compile-module-info-plugin") version "2.0"
}

dependencies {
    implementation(project(":base"))
    testImplementation("org.lz4:lz4-java:1.8.0")
}

tasks.compileJava {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
        )
    )
}

tasks.create<Jar>("bootJar") {
    destinationDirectory.set(rootProject.layout.buildDirectory)
    archiveFileName.set("japp-boot.jar")

    dependsOn(configurations.runtimeClasspath)

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { zipTree(it) })
}
