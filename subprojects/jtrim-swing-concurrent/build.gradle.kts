plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Swing Concurrent")
}

dependencies {
    api(project(":jtrim-ui-concurrent"))
    testImplementation(project(":test-jtrim-concurrent"))
}
