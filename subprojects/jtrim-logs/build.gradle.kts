plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Logs")
}

dependencies {
    api(project(":jtrim-collections"))
}
