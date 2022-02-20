plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Query")
}

dependencies {
    api(project(":jtrim-cache"))
    api(project(":jtrim-executor"))
    testImplementation(project(":test-jtrim-utils"))
}
