rootProject.name = "jtrim"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("java", providers.gradleProperty("compileJavaVersion").getOrElse("8"))
            version("javadocVersion", providers.gradleProperty("javadocJavaVersion").getOrElse("17"))

            version("checkstyle", "9.2.1")
            version("jacoco", "0.8.7")

            alias("junit").to("junit:junit:4.13.2")
            alias("mockitoCore").to("org.mockito:mockito-core:1.10.19")

            bundle("testLibs", listOf("junit", "mockitoCore"))
        }
    }
}

addSubprojects(File(rootDir, "subprojects"))
addSubprojects(File(rootDir, "subprojects-internal"))

fun addSubprojects(parentDir: File) {
    parentDir
            .listFiles()
            ?.filter { candidate ->
                candidate.isDirectory && File(candidate, "build.gradle.kts").exists()
            }
            ?.forEach { dir ->
                val subprojectName = dir.name
                include(subprojectName)
                project(":${subprojectName}").projectDir = dir
            }
}
