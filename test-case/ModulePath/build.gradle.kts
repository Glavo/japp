dependencies {
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.10.1")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.14.0")
}

tasks.jar {
    manifest.attributes(
        "Automatic-Module-Name" to "org.glavo.japp.testcase.modulepath"
    )
}