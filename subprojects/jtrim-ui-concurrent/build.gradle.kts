plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim UI Concurrent")
}

dependencies {
    api(project(":jtrim-access"))
    testImplementation(project(":test-jtrim-ui"))
}
