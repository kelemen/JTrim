plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Util")
}

dependencies {
    testImplementation(project(":test-jtrim-utils"))
}
