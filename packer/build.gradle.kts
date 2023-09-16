plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(Dependencies.JSON)
}

tasks.compileJava {
    // TODO: Java 8
    options.release.set(9)
}

tasks.jar {
    manifest.attributes(
        "Main-Class" to "org.glavo.japp.packer.Packer"
    )
}