package org.jtrim2.build

import java.nio.file.Files
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Too complicated to track.")
open class GeneratePackageListTask @Inject constructor(
    layout: ProjectLayout,
    objects: ObjectFactory,
    providers: ProviderFactory
) : DefaultTask() {

    private val packageList = providers.provider {
        val packages = HashSet<String>()
        collectPackageListFromSources(project, packages)
        packages
    }

    @Input
    val moduleName: Provider<String> = providers.provider { ProjectUtils.getModuleName(project) }

    @OutputFile
    val packageListFile: RegularFileProperty = objects.fileProperty()
        .value(layout.file(layout.buildDirectory.map { it.asFile.withChildren(name, "package-list").toFile() }))

    @TaskAction
    fun generatePackageList() {
        val sortedPackages = ArrayList(packageList.get())
        sortedPackages.sort()
        sortedPackages.add("")

        sortedPackages.add(0, "module:${moduleName.get()}")

        Files.writeString(packageListFile.get().asFile.toPath(), sortedPackages.joinToString("\n"))
    }

    companion object {
        const val DEFAULT_TASK_NAME = "mainPackageList"

        private fun collectPackageListFromSources(project: Project, result: MutableSet<String>) {
            if (!ProjectUtils.isReleasedProject(project)) {
                return
            }

            val java = ProjectUtils.tryGetJava(project) ?: return

            val sourceSet = java.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            for (sourceRoot in sourceSet.allJava.srcDirs) {
                PackageUtils.collectPackageListFromSourceRoot(sourceRoot.toPath(), result)
            }
        }
    }
}
