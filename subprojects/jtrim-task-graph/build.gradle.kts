plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Task Graph")
}

dependencies {
    api(project(":jtrim-executor"))
    testImplementation(project(":test-jtrim-utils"))
}
