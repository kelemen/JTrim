package org.jtrim2.build

import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault

private const val VERSION_FILE = "version.txt"

@DisableCachingByDefault(because = "We are always pushing new changes")
open class FinalReleaseTask @Inject constructor(private val objects: ObjectFactory) : DefaultTask() {
    @Internal
    @ServiceReference(GitRepoService.PROJECT_SERVICE_NAME)
    val repoServiceRef = objects.property<GitRepoService>()

    @Input
    val projectBaseVersion = objects.property<String>()

    @Input
    val projectVersion = objects.property<String>()

    @TaskAction
    fun finalizeRelease() {
        val projectVersion = projectVersion.get()
        val projectBaseVersion = projectBaseVersion.get()

        val repoService = repoServiceRef.get()
        val git = repoService.git(objects).jgit()

        val statusCommand = git.status()
        val status = statusCommand.call()

        if (status.untracked.isNotEmpty()) {
            throw RuntimeException("There are untracked files in the repository and so the release cannot be completed. Revert the changes already done manually.")
        }
        if (!status.isClean) {
            throw RuntimeException("The repository is not clean (contains uncommitted changes) and so the release cannot be completed. Revert the changes already done manually.")
        }

        val tagCommand = git.tag()
        tagCommand.name = "v$projectVersion"
        tagCommand.message = "Release of JTrim $projectVersion"
        tagCommand.call()

        val nextVersion = setNextVersion(repoService.repoRoot, projectBaseVersion)

        val addCommand = git.add()
        addCommand.addFilepattern(VERSION_FILE)
        addCommand.isUpdate = true
        addCommand.call()

        val commitCommand = git.commit()
        commitCommand.message = "Set the version to $nextVersion"
        commitCommand.call()

        println("New Release: $projectVersion, Next version = $nextVersion")
    }

    private fun setNextVersion(repoRoot: File, baseVersion: String): String {
        val nextVersion = incVersion(baseVersion)

        val versionFile = repoRoot.toPath().resolve(VERSION_FILE)
        Files.writeString(versionFile, nextVersion)
        return nextVersion
    }

    private fun incVersion(version: String): String {
        val sepIndex = version.lastIndexOf('.')
        val prefix = version.substring(0, sepIndex)
        val patchVersion = version.substring(sepIndex + 1).toInt()
        return prefix + '.' + (patchVersion + 1)
    }
}
