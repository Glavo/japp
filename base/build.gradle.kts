import java.util.Properties

tasks.compileJava {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
        )
    )
}

val jappPropertiesFile = rootProject.layout.buildDirectory.file("japp.properties").get().asFile

 tasks.create("generateJAppProperties") {
    outputs.file(jappPropertiesFile)
    doLast {
        val properties = Properties()
        properties["Project-Directory"] = rootProject.layout.projectDirectory.asFile.absolutePath
        jappPropertiesFile.writer().use { writer ->
            properties.store(writer, null)
        }
    }
}

tasks.processResources {
    dependsOn("generateJAppProperties")

    into("org/glavo/japp") {
        from(jappPropertiesFile)
    }
}