plugins {
    id("java")
    // id("com.github.johnrengelman.shadow") version "8.1.1"
}


allprojects {
    apply {
        plugin("java")
    }

    group = "org.glavo"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    tasks.test {
        useJUnitPlatform()
    }
}


dependencies {
    testImplementation("org.lz4:lz4-java:1.8.0")
}

tasks.compileJava {
    // TODO: Java 8
    sourceCompatibility = "9"
    targetCompatibility = "9"

    options.compilerArgs.addAll(
        listOf(
            "--add-exports=java.base/jdk.internal.loader=org.glavo.japp",
            "--add-exports=java.base/jdk.internal.module=org.glavo.japp",
        )
    )
}

tasks.jar {
    doLast {
        tasks.jar.get().archiveFile.get().asFile.copyTo(
            project.layout.buildDirectory.get().file("japp.jar").asFile,
            overwrite = true
        )
    }

    manifest {
        attributes(
            // In the early stages we isolate the configuration in the project directory
            "JApp-Home" to project.layout.projectDirectory.file(".japp").asFile.absolutePath
        )
    }
}