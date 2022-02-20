plugins {
    java
}

val gradleDir = projectDir.toPath().parent.resolve("gradle")
apply(from = gradleDir.resolve("repositories.gradle.kts"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-Xlint")
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())

    val jgitVersion = "6.0.0.202111291000-r"

    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    implementation("org.eclipse.jgit:org.eclipse.jgit.gpg.bc:$jgitVersion")

    implementation("commons-io:commons-io:2.11.0")
}
