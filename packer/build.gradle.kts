plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20230618")
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