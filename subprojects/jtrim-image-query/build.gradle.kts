plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Image Query")
}

dependencies {
    api(project(":jtrim-image"))
    api(project(":jtrim-query"))
    testImplementation(project(":test-jtrim-ui"))
}
