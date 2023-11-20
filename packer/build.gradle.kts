plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":base"))
    implementation(project(":boot"))
    implementation(project(":launcher"))

    implementation(Deps.LZ4)
    implementation(Deps.ZSTD_JNI)
}

tasks.jar {
    manifest.attributes(
        "Main-Class" to "org.glavo.japp.packer.JAppPacker"
    )
}

tasks.shadowJar {
    outputs.file(rootProject.layout.buildDirectory.file("japp-packer.jar"))
    doLast {
        copy {
            from(this@shadowJar.archiveFile)
            into(rootProject.layout.buildDirectory)
            rename(".*", "japp-packer.jar")
        }
    }
}
