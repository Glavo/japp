dependencies {
    implementation(Dependencies.JSON)
}

tasks.compileJava {
    // TODO: Java 8
    options.release.set(9)
}
