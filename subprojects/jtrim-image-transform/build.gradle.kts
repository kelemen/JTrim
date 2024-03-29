plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Image Transform")
}

dependencies {
    api(project(":jtrim-image"))
    api(project(":jtrim-concurrent"))
    testImplementation(project(":test-jtrim-ui"))
}
