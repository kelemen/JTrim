plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Query")
}

dependencies {
    api(project(":jtrim-cache"))
    api(project(":jtrim-executor"))
    implementation(libs.slf4jApi)
    testImplementation(project(":test-jtrim-utils"))
}
