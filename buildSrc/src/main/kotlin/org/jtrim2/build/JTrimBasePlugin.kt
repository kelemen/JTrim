package org.jtrim2.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.jtrim2.build.credentials.CredentialUtils

private const val FORCED_EVALUATE_TASK_NAME = "evaluate"

class JTrimBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (ProjectUtils.runningWithSupervision(project).get()) {
            CredentialUtils.setupCredentialProvider()
        }

        ProjectUtils.applyPlugin(project, "base") // To add "clean" task

        Versions.setVersion(project)
        ProjectUtils.applyScript(project, "repositories.gradle.kts")

        project.extensions.add("projectInfo", JTrimProjectInfo(project))
        project.tasks.register(FORCED_EVALUATE_TASK_NAME)
    }

    companion object {
        private fun qualifiedDependency(project: Project, relativeTaskName: String): String {
            return "${project.path}:$relativeTaskName"
        }

        fun requireTaskOfProject(task: Task, requiredProject: Project, relativeTaskName: String) {
            if (requiredProject.parent == null) {
                return
            }
            task.dependsOn(qualifiedDependency(requiredProject, relativeTaskName))
        }

        fun requireEvaluateSubprojects(task: Task, parent: Project = task.project) {
            requireSubprojectsTask(task, parent, FORCED_EVALUATE_TASK_NAME)
        }

        fun requireSubprojectsTask(task: Task, parent: Project, dependency: String) {
            parent.subprojects { requireTaskOfProject(task, this, dependency) }
        }
    }
}
