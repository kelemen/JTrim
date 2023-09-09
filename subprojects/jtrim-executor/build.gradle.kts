plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Executor")
}

dependencies {
    api(project(":jtrim-concurrent"))
    implementation(libs.slf4jApi)
    testImplementation(project(":test-jtrim-concurrent"))
}
