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
    implementation(Deps.JSON)
}

tasks.compileJava {
    // TODO: Java 8
    options.release.set(9)
}

tasks.jar {
    manifest.attributes(
        "Main-Class" to "org.glavo.japp.Packer"
    )
}