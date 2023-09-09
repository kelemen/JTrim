package org.jtrim2.build

import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

class JTrimJavaBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        ProjectUtils.applyPlugin<JTrimBasePlugin>(project)

        project.pluginManager.apply("java-library")
        configureJava(project)

        setupTravis(project)

        project.afterEvaluate {
            // We are setting this, so the pom.xml will be generated properly
            System.setProperty("line.separator", "\n")
        }
    }

    private fun configureJava(project: Project) {
        val java = ProjectUtils.java(project)

        java.toolchain { languageVersion.set(ProjectUtils.getCompileJavaVersion(project)) }

        val tasks = project.tasks
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs = listOf("-Xlint:all,-requires-automatic")
        }

        val javaExt = project.the<JavaPluginExtension>()
        javaExt.withSourcesJar()
        javaExt.withJavadocJar()

        setDefaultDependencies(project)
    }

    private fun setDefaultDependencies(project: Project) {
        project.dependencies
            .add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, ProjectUtils.getBundle(project, "testLibs"))
    }

    private fun setupTravis(project: Project) {
        if (!project.hasProperty("printTestErrorXmls")) {
            return
        }

        project.tasks.named<Test>(JavaPlugin.TEST_TASK_NAME) {
            ignoreFailures = true

            val test = this
            doLast {
                var numberOfFailures = 0
                val destination = test.reports.junitXml.outputLocation.get().asFile
                for (file in emptyForNull(destination.listFiles())) {
                    val nameLowerCase = file.name.lowercase()
                    if (nameLowerCase.startsWith("test-") && nameLowerCase.endsWith(".xml")) {
                        if (printIfFailing(file)) {
                            numberOfFailures++
                        }
                    }
                }

                if (numberOfFailures > 0) {
                    throw RuntimeException("There were $numberOfFailures failing test classes.")
                }
            }
        }
    }

    private fun printIfFailing(file: File): Boolean {
        try {
            val content = file.readText(Charset.defaultCharset())
            if (content.contains("</failure>")) {
                println(
                    """
                    Failing test ${file.name}:
                    $content
                    
                    
                    """.trimIndent()
                )
                return true
            }
        } catch (ex: IOException) {
            // ignore silently
        }
        return false
    }
}

private fun emptyForNull(array: Array<File>?): List<File> {
    return array?.asList() ?: emptyList()
}
