rootProject.name = "jtrim-buildsrc"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("java", providers.gradleProperty("buildJavaVersion").getOrElse("17"))
        }
    }
}
