plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Cache")
}

dependencies {
    api(project(":jtrim-collections"))
    testImplementation(project(":test-jtrim-utils"))
}
