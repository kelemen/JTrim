rootProject.name = "jtrim-buildsrc"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        // If we named this "libs" as well, then Idea will think it conflicts with "libs" in the main project.
        create("buildLibs") {
            version("java", providers.gradleProperty("buildJavaVersion").getOrElse("11"))
        }
    }
}
