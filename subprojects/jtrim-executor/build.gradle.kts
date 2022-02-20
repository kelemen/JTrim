plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Executor")
}

dependencies {
    api(project(":jtrim-concurrent"))
    testImplementation(project(":test-jtrim-concurrent"))
}
