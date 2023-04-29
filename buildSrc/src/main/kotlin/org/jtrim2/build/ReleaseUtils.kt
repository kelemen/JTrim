package org.jtrim2.build

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

object ReleaseUtils {
    private const val RELEASE_TASK_NAME = "release"
    private const val DO_RELEASE_PROPERTY = "doRelease"

    fun isRelease(project: Project): Boolean {
        return project.hasProperty(DO_RELEASE_PROPERTY)
    }

    fun setupMainReleaseTask(project: Project) {
        setupPublishDocs(project)
        project.tasks.register<FinalReleaseTask>(RELEASE_TASK_NAME) {
            group = PublishingPlugin.PUBLISH_TASK_GROUP
            description = "Releases JTrim if the $DO_RELEASE_PROPERTY property is defined."

            if (!isRelease(project)) {
                doFirst {
                    throw RuntimeException(
                        "You must specify the '-P" + DO_RELEASE_PROPERTY
                                + "' argument to execute the release task."
                    )
                }
            }

            projectVersion.set(getVersionRef(project))
            projectBaseVersion.set(project.providers.provider { Versions.getVersionBase(project) })
        }
    }

    fun setupPublishDocs(project: Project) {
        project.tasks.register<ReleaseApiDocTask>("releaseApiDoc") {
            group = PublishingPlugin.PUBLISH_TASK_GROUP
            description = "Copies and commites the Javadoc files into the given git repository."

            val javadocRef = project
                .tasks
                .named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME)

            dependsOn(javadocRef)

            targetBranchName.set(project.name)
            javadocSourceDir.set(project.layout.dir(javadocRef.map { checkNotNull(it.destinationDir) }))

            projectDisplayName.set(ProjectUtils.getProjectInfo(project).displayName)
            projectVersion.set(getVersionRef(project))
        }
    }

    private fun getVersionRef(project: Project): Provider<String> {
        return project.providers.provider { project.version.toString() }
    }

}
