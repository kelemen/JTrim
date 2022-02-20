plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Producer-Consumer Management")
}

dependencies {
    api(project(":jtrim-executor"))

    testImplementation(project(":test-jtrim-concurrent"))
}
