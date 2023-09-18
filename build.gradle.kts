import org.gradle.api.artifacts.dsl.Dependencies

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
        testImplementation(platform("org.junit:junit-bom:5.9.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    tasks.test {
        useJUnitPlatform()
    }
}

dependencies {
    // In order to create a prototype more quickly, temporarily store the metadata in JSON format.
    // TODO: Will be replaced by a more compact binary format in the future
    implementation(Deps.JSON)

    implementation(Deps.GLOB)
}

tasks.compileJava {
    // TODO: Java 8
    sourceCompatibility = "9"
    targetCompatibility = "9"

    options.compilerArgs.add("--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED")
}

tasks.jar {
    manifest.attributes(
        "Main-Class" to "org.glavo.japp.Packer"
    )
}