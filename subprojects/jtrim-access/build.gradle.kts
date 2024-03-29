plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Access")
}

dependencies {
    api(project(":jtrim-executor"))
    api(project(":jtrim-property"))
    testImplementation(project(":test-jtrim-concurrent"))
}
