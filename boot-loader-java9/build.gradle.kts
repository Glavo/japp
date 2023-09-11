tasks.compileJava {
    sourceCompatibility = "9"
    targetCompatibility = "9"

    options.compilerArgs.add("--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED")
}