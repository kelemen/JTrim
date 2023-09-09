package org.jtrim2.build

import java.io.File
import java.util.function.Function
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.reporting.Report
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoReportsContainer

class JTrimGroupPlugin @Inject constructor(private val toolchainService: JavaToolchainService) : Plugin<Project> {

    override fun apply(project: Project) {
        ProjectUtils.applyPlugin<JTrimBasePlugin>(project)

        ReleaseUtils.setupMainReleaseTask(project)
        setupJavadoc(project)
        setupJacoco(project)
    }

    private fun setupJavadoc(
        project: Project,
        subprojectsRef: Provider<List<Project>> = ProjectUtils.releasedSubprojects(project)
    ) {
        val tasks = project.tasks

        val javadocClasspathRef = project.configurations.register("javadocClasspath")
        project.gradle.projectsEvaluated {
            val dependencies = project.dependencies
            subprojectsRef.get().forEach {
                dependencies.add(javadocClasspathRef.name, it)
            }
        }

        val javadocRef = tasks.register<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
            title = "JTrim " + Versions.getVersion(project) + " - All modules"
            setDestinationDir(File(project.buildDir, "merged-javadoc"))

            exclude("/module-info.java")
            source(subprojectsRef.map { subprojects: List<Project> ->
                subprojects.flatMap { sourceDirs(it, SourceSet.MAIN_SOURCE_SET_NAME) }
            })

            dependsOn(subprojectsRef.map { projects: List<Project> ->
                projects.map { "${it.path}:${JavaPlugin.JAR_TASK_NAME}" }
            })

            modularity.inferModulePath.set(false)

            classpath = project.objects
                .fileCollection()
                .from(javadocClasspathRef)

            JTrimJavaPlugin.setCommonJavadocConfig(this, toolchainService, emptyList())
            JTrimBasePlugin.requireEvaluateSubprojects(this)
        }
        tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME).configure { dependsOn(javadocRef) }

        val checkUniquePackages = tasks.register<CheckUniquePackagesTask>("checkUniquePackages") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
        }
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { dependsOn(checkUniquePackages) }
    }

    private fun setupJacoco(
        project: Project,
        subprojectsRef: Provider<List<Project>> = ProjectUtils.releasedSubprojects(project)
    ) {
        JTrimJavaPlugin.applyJacoco(project)
        val mainSourceSets = subprojectsRef.map { subprojects: List<Project> ->
            subprojects.map { sourceSet(it, SourceSet.MAIN_SOURCE_SET_NAME) }
        }
        val dependenciesRef = subprojectsRef.map { subprojects: List<Project> ->
            subprojects.map { "${it.path}:${JavaPlugin.TEST_TASK_NAME}" }
        }

        project.tasks.register<JacocoReport>("jacocoTestReport").configure {
            JTrimBasePlugin.requireEvaluateSubprojects(this)
            dependsOn(dependenciesRef)

            sourceDirectories.from(mainSourceSets.map { sourceSets: List<SourceSet> ->
                sourceSets.flatMap { it.allSource.srcDirs }
            })
            classDirectories.from(mainSourceSets.map { sourceSets: List<SourceSet> ->
                sourceSets.map { it.output }
            })
            executionData.from(subprojectsRef.map { subprojects: List<Project> ->
                subprojects.flatMap { subproject: Project ->
                    val taskRef = subproject.tasks.tryGetTaskRef<JacocoReport>("jacocoTestReport")
                        ?: return@flatMap emptyList<File>()

                    taskRef.get()
                        .executionData
                        .files
                        .filter { it.exists() }
                }
            })

            val reportDefs = listOf<ReportDef<*>>(
                ReportDef({ it.html }, { it.entryPoint }, true),
                ReportDef({ it.xml }, { it.outputLocation.get().asFile }, false),
                ReportDef({ it.csv }, { it.outputLocation.get().asFile }, false)
            )

            reports {
                val reportsContainer = this
                reportDefs.forEach { it.getReport(reportsContainer).required.set(it.isDefaultRequired) }
            }

            val jacocoReport = this
            doLast {
                reportDefs.forEach {
                    val reportUri = it.getTarget(jacocoReport.reports).toURI()
                    println("Successfully generated Jacoco report to $reportUri")
                }
            }
        }
    }
}

private class ReportDef<R : Report>(
    private val reportProvider: Function<JacocoReportsContainer, R>,
    private val targetProvider: Function<R, File>,
    val isDefaultRequired: Boolean
) {

    fun getReport(reportsContainer: JacocoReportsContainer): R {
        return reportProvider.apply(reportsContainer)
    }

    fun getTarget(reportsContainer: JacocoReportsContainer): File {
        return targetProvider.apply(getReport(reportsContainer))
    }
}

private fun sourceSets(project: Project): SourceSetContainer =
    ProjectUtils.java(project).sourceSets

private fun sourceSet(project: Project, sourceSetName: String): SourceSet =
    sourceSets(project).getByName(sourceSetName)

private fun sourceDirs(project: Project, sourceSetName: String): Collection<File> =
    sourceSet(project, sourceSetName).allSource.srcDirs

