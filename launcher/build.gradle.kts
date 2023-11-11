plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":base"))
}

tasks.jar {
    manifest.attributes(
        "Main-Class" to "org.glavo.japp.launcher.Launcher",
        "JApp-Boot" to project(":boot").tasks.getByName<Jar>("bootJar").archiveFile.get().asFile.absolutePath
    )
}