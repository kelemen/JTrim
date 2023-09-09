plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Swing Properties")
}

dependencies {
    api(project(":jtrim-swing-concurrent"))
    implementation(libs.slf4jApi)
    testImplementation(project(":test-jtrim-ui"))
}
