package org.jtrim2.build

import java.nio.file.Files
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class CheckStyleConfigurer(private val project: Project, private val type: String) {
    fun configure() {
        project.pluginManager.apply("checkstyle")

        project.extensions.configure<CheckstyleExtension> {
            configFile = checkStyeConfig(null).toFile()
            toolVersion = ProjectUtils.getVersion(project, "checkstyle")
        }

        project.tasks.withType<Checkstyle>().configureEach {
            exclude("/module-info.java")

            val sourceSetName = getSourceSetName(this)
            val configCandidate = checkStyeConfig(sourceSetName)
            if (Files.isRegularFile(configCandidate)) {
                configFile = configCandidate.toFile()
            }

            val java = ProjectUtils.java(project)
            val sourceSet = java.sourceSets.findByName(sourceSetName)
            if (sourceSet != null) {
                classpath = sourceSet.runtimeClasspath
            }
        }
    }

    private fun getSourceSetName(task: Checkstyle): String {
        val expectedPrefix = "checkstyle"
        val name = task.name

        if (!name.startsWith(expectedPrefix)) {
            return name
        }

        val sourceSetName = name.substring(expectedPrefix.length)
        return lowerCaseFirst(sourceSetName)
    }

    private fun lowerCaseFirst(str: String): String {
        if (str.isEmpty()) {
            return str
        }

        val firstCh = str[0]
        val lowFirstCh = Character.toLowerCase(firstCh)

        return if (firstCh == lowFirstCh) {
            str
        } else {
            return "${lowFirstCh}${str.substring(1)}"
        }
    }

    private fun checkStyeConfig(sourceSetName: String?): Path {
        val fileName = "check-style" +
                (if (sourceSetName != null) "-$sourceSetName" else "") +
                (if (type.isNotEmpty()) ".${type}" else "") +
                ".xml"
        return project.rootDir.withChildren("gradle", fileName)
    }
}
