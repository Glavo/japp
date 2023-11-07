import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":boot"))
    implementation(project(":launcher"))
}

tasks.jar {
    manifest.attributes(
        "Main-Class" to "org.glavo.japp.packer.JAppPacker",
        "JApp-Launcher" to project(":launcher").tasks.getByName<ShadowJar>("shadowJar").archiveFile.get().asFile.absolutePath
    )
}

tasks.shadowJar {
    outputs.file(rootProject.layout.buildDirectory.file("packer.jar"))
    doLast {
        copy {
            from(this@shadowJar.archiveFile)
            into(rootProject.layout.buildDirectory)
            rename(".*", "packer.jar")
        }
    }
}

