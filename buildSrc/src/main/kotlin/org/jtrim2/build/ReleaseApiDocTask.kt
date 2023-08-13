package org.jtrim2.build

import java.io.File
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault

private const val API_DIR_NAME = "api"

@DisableCachingByDefault(because = "We are always pushing new changes")
open class ReleaseApiDocTask @Inject constructor(private val objects: ObjectFactory) : DefaultTask() {
    @ServiceReference(GitRepoService.API_DOCS_SERVICE_NAME)
    val apiDocsRepoServiceRef = objects.property<GitRepoService>()

    @Input
    val targetBranchName = objects.property<String>()

    @InputDirectory
    val javadocSourceDir: DirectoryProperty = objects.directoryProperty()

    @Input
    val projectDisplayName = objects.property<String>()

    @Input
    val projectVersion = objects.property<String>()

    @TaskAction
    fun releaseApiDoc() {
        val branchNAme = targetBranchName.get()
        val sourceDir = javadocSourceDir.get().asFile
        val commitMessage = getCommitMessage()

        val service = apiDocsRepoServiceRef.get()
        val repoRoot: File = service.repoRoot

        val git = service.git(objects)

        git.clean()

        git.checkoutBranchMaybeRemoteOrDefault(branchNAme, "master")
        prepareContent(sourceDir, File(repoRoot, API_DIR_NAME))
        git.addAllInDir(repoRoot.toPath(), API_DIR_NAME)
        git.commmitAll(commitMessage)
    }

    private fun getCommitMessage(): String = "Added API doc for ${projectDisplayName.get()} ${projectVersion.get()}."
}

private fun prepareContent(javadocSourceDir: File, apiDocPath: File) {
    FileUtils.deleteDirectory(apiDocPath)
    FileUtils.copyDirectory(javadocSourceDir, apiDocPath, false)
}
