plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Swing Query")
}

dependencies {
    api(project(":jtrim-ui-query"))
    api(project(":jtrim-swing-concurrent"))
    testImplementation(project(":test-jtrim-ui"))
}
