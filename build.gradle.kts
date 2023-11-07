plugins {
    id("java")
}

allprojects {
    apply {
        plugin("java")
    }

    group = "org.glavo"
    version = "0.1.0-SNAPSHOT"

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

    tasks.compileJava {
        // TODO: Java 8
        sourceCompatibility = "9"
        targetCompatibility = "9"
    }

    tasks.jar {
        manifest {
            attributes(
                // In the early stages we isolate the configuration in the project directory
                "JApp-Home" to rootProject.layout.projectDirectory.file(".japp").asFile.absolutePath
            )
        }
    }
}

tasks.create("buildAll") {
    dependsOn(
        ":boot:jar", ":launcher:shadowJar", ":packer:shadowJar"
    )
}

defaultTasks("buildAll")
