package org.jtrim2.build

import java.io.File
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class GitRepoService : BuildService<GitRepoService.Parameters>, AutoCloseable {
    val repoRoot: File = parameters.repositoryLocation.get()
    private val repository: Repository = FileRepositoryBuilder.create(File(repoRoot, ".git"))

    fun git(objects: ObjectFactory?): GitWrapper {
        return GitWrapper(objects!!, repository)
    }

    override fun close() {
        repository.close()
    }

    interface Parameters : BuildServiceParameters {
        val repositoryLocation: Property<File>
    }

    companion object {
        const val PROJECT_SERVICE_NAME = "default-git-repo-service"
        const val API_DOCS_SERVICE_NAME = "api-docs-git-repo-service"

        fun register(project: Project, name: String, locationConfig: Action<in Property<File>>) {
            register(project.gradle, name, locationConfig)
        }

        fun register(gradle: Gradle, name: String, locationConfig: Action<in Property<File>>) {
            gradle.sharedServices.registerIfAbsent(name, GitRepoService::class.java) {
                maxParallelUsages.set(1)
                locationConfig.execute(parameters.repositoryLocation)
            }
        }

        fun getService(project: Project, name: String): Provider<GitRepoService> {
            return getService(project.gradle, name)
        }

        @Suppress("UNCHECKED_CAST")
        fun getService(gradle: Gradle, name: String): Provider<GitRepoService> {
            return gradle.sharedServices
                .registrations
                .named(name)
                .get()
                .service as Provider<GitRepoService>
        }
    }
}
