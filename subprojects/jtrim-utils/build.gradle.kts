plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Util")
}

dependencies {
    implementation(libs.slf4jApi)

    testImplementation(project(":test-jtrim-utils"))
}
