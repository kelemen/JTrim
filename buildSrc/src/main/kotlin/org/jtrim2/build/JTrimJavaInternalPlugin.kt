package org.jtrim2.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.withType

class JTrimJavaInternalPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        ProjectUtils.applyPlugin<JTrimJavaBasePlugin>(project)
        CheckStyleConfigurer(project, "internal").configure()

        project.tasks.withType<Javadoc>().configureEach { isEnabled = false }
    }
}
