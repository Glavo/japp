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

    tasks.compileJava {
        // TODO: Java 8
        sourceCompatibility = "9"
        targetCompatibility = "9"
    }

    tasks.jar {
        manifest {
            attributes(
                // In the early stages we isolate the configuration in the project directory
                "Project-Directory" to rootProject.layout.projectDirectory.asFile.absolutePath,
            )
        }
    }
}

tasks.create("buildAll") {
    dependsOn(
        ":boot:bootJar", ":launcher:shadowJar", ":packer:shadowJar"
    )
}

defaultTasks("buildAll")

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation(Deps.LZ4)
    testImplementation(Deps.ZSTD_JNI)

    testImplementation(project(":base"))
    testImplementation(project(":boot"))
    testImplementation(project(":launcher"))
    testImplementation(project(":packer"))
}

tasks.compileTestJava {
    options.compilerArgs.add("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs = listOf(
        "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED"
    )
}
