package org.jtrim2.build

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Too complicated to track.")
open class CheckUniquePackagesTask : DefaultTask() {
    @TaskAction
    fun checkPackages() {
        // FIXME: Accessing the project instance will interfere with the configuration cache in the future.
        val rootProject = project

        val allPackages: MutableMap<String, String> = HashMap()
        for (subProject in rootProject.subprojects) {
            val projectName = subProject.path
            findAllPackagesOfProject(subProject).forEach { pckg ->
                val prevOwner = allPackages.putIfAbsent(pckg, projectName)
                if (prevOwner != null) {
                    throw IllegalStateException("Package \"$pckg\" was found in multiple projects: $prevOwner, $projectName")
                }
            }
        }
    }

    private fun findAllPackagesOfProject(project: Project): Set<String> {
        val packages = HashSet<String>()
        collectPackageListFromSources(project, packages)
        return packages
    }

    private fun collectPackageListFromSources(project: Project, result: MutableSet<String>) {
        val java = ProjectUtils.tryGetJava(project) ?: return
        for (sourceSet in java.sourceSets) {
            for (sourceRoot in sourceSet.allJava.srcDirs) {
                PackageUtils.collectPackageListFromSourceRoot(sourceRoot.toPath(), result)
            }
        }
    }
}
