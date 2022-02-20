plugins {
    `jtrim-java-internal`
}

dependencies {
    api(project(":jtrim-logs"))
    api(project(":jtrim-concurrent"))

    api(libs.bundles.testLibs)
}
