plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Event Tracking")
}

dependencies {
    api(project(":jtrim-executor"))
    testImplementation(project(":test-jtrim-concurrent"))
}
