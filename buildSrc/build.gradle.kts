plugins {
    `kotlin-dsl`
    idea
}

val gradleDir = projectDir.toPath().parent.resolve("gradle")
apply(from = gradleDir.resolve("repositories.gradle.kts"))

val javaVersionStr = buildLibs.versions.java.get()
val javaVersion = JavaLanguageVersion.of(javaVersionStr)

idea.module.jdkName = javaVersionStr

// We are setting this for now to avoid Gradle complaining that the target JDK of Java and Kotlin are different.
java.toolchain.languageVersion.set(javaVersion)

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())

    val jgitVersion = "6.0.0.202111291000-r"

    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    implementation("org.eclipse.jgit:org.eclipse.jgit.gpg.bc:$jgitVersion")

    implementation("commons-io:commons-io:2.11.0")
}
