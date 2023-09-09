plugins {
    `jtrim-java-internal`
}

dependencies {
    api(project(":jtrim-logs"))
    api(project(":jtrim-concurrent"))

    implementation(libs.slf4jJdk)

    api(libs.bundles.testLibs)
}
