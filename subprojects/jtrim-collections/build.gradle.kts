plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Collections")
}

dependencies {
    api(project(":jtrim-utils"))
    testImplementation(project(":test-jtrim-utils"))
}
