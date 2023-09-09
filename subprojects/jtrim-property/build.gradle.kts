plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Properties")
}

dependencies {
    api(project(":jtrim-executor"))
    implementation(libs.slf4jApi)
    testImplementation(project(":test-jtrim-utils"))
}
