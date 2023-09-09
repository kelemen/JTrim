package org.jtrim2.build

import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class JTrimRootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        configureDefaultGitRepoService(project)
        configureApiDocsGitRepoService(project)

        ProjectUtils.applyPlugin<JTrimGroupPlugin>(project)

        project.extensions.add("development", JTrimDevelopment::class.java)
        project.extensions.add("license", LicenseInfo::class.java)

        project.tasks.register<DownloadFileTask>("updateJdkElementList") {
            description = "Updates the JDK element-list file we are using to link to from the default location."

            val elementListFileName = "element-list"

            sourceUrl.set("${JTrimJavaPlugin.getJavadocUrl(project)}${elementListFileName}")

            val destinationDir = JTrimJavaPlugin.getExternalJavadocResourcesDir(project, "java")
            destinationFile.set(project.file(destinationDir).resolve(elementListFileName))
        }
    }

    private fun configureDefaultGitRepoService(project: Project) {
        val rootDir = project.rootDir
        GitRepoService.register(project, GitRepoService.PROJECT_SERVICE_NAME) { set(rootDir) }
    }

    private fun configureApiDocsGitRepoService(project: Project) {
        val apiDocsPathStrRef = project.providers.gradleProperty("releaseApiDocRepo")
        GitRepoService.register(project, GitRepoService.API_DOCS_SERVICE_NAME) {
            set(apiDocsPathStrRef.map { File(it) })
        }
    }
}
