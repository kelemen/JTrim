plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Concurrent")
}

dependencies {
    api(project(":jtrim-collections"))
    implementation(libs.slf4jApi)
    testImplementation(project(":test-jtrim-concurrent"))
}
