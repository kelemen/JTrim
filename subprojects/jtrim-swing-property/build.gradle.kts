plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Swing Properties")
}

dependencies {
    api(project(":jtrim-swing-concurrent"))
    testImplementation(project(":test-jtrim-ui"))
}
