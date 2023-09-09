plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim UI Query")
}

dependencies {
    api(project(":jtrim-query"))
    api(project(":jtrim-ui-concurrent"))
    implementation(libs.slf4jApi)
    testImplementation(project(":test-jtrim-ui"))
}
