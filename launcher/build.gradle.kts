import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":boot"))
}

tasks.jar {
    manifest.attributes(
        "Main-Class" to "org.glavo.japp.launcher.Launcher",
        "JApp-Boot" to project(":boot").tasks.getByName<Jar>("jar").archiveFile.get().asFile.absolutePath
    )
}