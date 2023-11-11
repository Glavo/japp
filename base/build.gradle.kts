tasks.compileJava {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
        )
    )
}
