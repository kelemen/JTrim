plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Concurrent")
}

dependencies {
    api(project(":jtrim-collections"))
    testImplementation(project(":test-jtrim-concurrent"))
}
