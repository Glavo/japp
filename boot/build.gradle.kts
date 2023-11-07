dependencies {
    testImplementation("org.lz4:lz4-java:1.8.0")
}

tasks.compileJava {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports=java.base/jdk.internal.loader=org.glavo.japp.boot",
            "--add-exports=java.base/jdk.internal.module=org.glavo.japp.boot",
        )
    )
}