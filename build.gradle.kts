plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
}

tasks.create("buildAll") {
    dependsOn(
        ":boot:bootJar", ":shadowJar"
    )
}

defaultTasks("buildAll")

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation(Deps.ZSTD_JNI)

    LWJGL.addDependency(this, "testImplementation", "lwjgl")
    LWJGL.addDependency(this, "testImplementation", "lwjgl-xxhash")
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

dependencies {
    implementation(project(":base"))
    implementation(project(":boot"))
    implementation(Deps.ZSTD_JNI)
}

tasks.jar {
    manifest.attributes(
        "Main-Class" to "org.glavo.japp.Main",
        "JApp-Boot" to project(":boot").tasks.getByName<Jar>("bootJar").archiveFile.get().asFile.absolutePath,
        "Add-Exports" to "java.base/jdk.internal.misc"
    )
}

tasks.shadowJar {
    outputs.file(rootProject.layout.buildDirectory.file("japp.jar"))
    doLast {
        copy {
            from(this@shadowJar.archiveFile)
            into(rootProject.layout.buildDirectory)
            rename(".*", "japp.jar")
        }
    }
}

