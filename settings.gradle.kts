rootProject.name = "jtrim"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("java", providers.gradleProperty("compileJavaVersion").getOrElse("11"))
            version("javadocVersion", providers.gradleProperty("javadocJavaVersion").getOrElse("17"))

            version("checkstyle", "9.2.1")
            version("jacoco", "0.8.7")

            val slf4jVersion = "1.7.36"
            library("slf4jApi", "org.slf4j:slf4j-api:${slf4jVersion}")
            library("slf4jJdk", "org.slf4j:slf4j-jdk14:${slf4jVersion}")

            library("junit", "junit:junit:4.13.2")
            library("mockitoCore", "org.mockito:mockito-core:5.4.0")

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
