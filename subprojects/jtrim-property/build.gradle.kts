plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Properties")
}

dependencies {
    api(project(":jtrim-executor"))
    testImplementation(project(":test-jtrim-utils"))
}
