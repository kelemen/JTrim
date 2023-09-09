plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Producer-Consumer Management")
}

dependencies {
    api(project(":jtrim-executor"))
    implementation(libs.slf4jApi)

    testImplementation(project(":test-jtrim-concurrent"))
}
