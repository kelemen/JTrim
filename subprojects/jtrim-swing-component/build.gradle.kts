plugins {
    `jtrim-java`
}

projectInfo {
    displayName.set("JTrim Swing Components")
}

dependencies {
    api(project(":jtrim-image-query"))
    api(project(":jtrim-image-transform"))
    api(project(":jtrim-swing-query"))
    api(project(":jtrim-swing-property"))
    implementation(libs.slf4jApi)
    testImplementation(project(":test-jtrim-ui"))
}
