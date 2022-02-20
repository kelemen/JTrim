plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Image")
}

dependencies {
    api(project(":jtrim-collections"))
    api(project(":jtrim-cache"))
    testImplementation(project(":test-jtrim-ui"))
}
