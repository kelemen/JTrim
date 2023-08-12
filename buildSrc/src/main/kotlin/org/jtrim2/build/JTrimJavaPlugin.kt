package org.jtrim2.build

import java.io.File
import java.util.function.Consumer
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.JavadocOfflineLink
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

private const val JAVADOC_8_URL_PATTERN_JDK = "https://docs.oracle.com/javase/\${version}/docs/api/"
private const val JAVADOC_11_URL_PATTERN_JDK = "https://docs.oracle.com/en/java/javase/\${version}/docs/api/"
private const val JAVADOC_JTRIM_URL = "https://www.javadoc.io/doc/\${group}/\${name}/\${version}/"

class JTrimJavaPlugin @Inject constructor(private val toolchainService: JavaToolchainService) : Plugin<Project> {
    override fun apply(project: Project) {
        ProjectUtils.applyPlugin<JTrimJavaBasePlugin>(project)

        ReleaseUtils.setupPublishDocs(project)

        applyJacoco(project)

        CheckStyleConfigurer(project, "").configure()
        MavenConfigurer(project).configure()

        configureJavadoc(project)
    }

    fun configureJavadoc(project: Project) {
        project.tasks.register<GeneratePackageListTask>(GeneratePackageListTask.DEFAULT_TASK_NAME)

        project.tasks.withType<Javadoc>().configureEach {
            val extraTaskDependencies = ProjectUtils
                .releasedSubprojects(project.rootProject)
                .map<List<String>> { projects: List<Project> ->
                    val packageListTaskNames: MutableList<String> = ArrayList()
                    projects.forEach(Consumer { projectDependency: Project ->
                        packageListTaskNames.add(projectDependency.path + ":" + JavaPlugin.JAR_TASK_NAME)
                        packageListTaskNames.add(projectDependency.path + ":" + GeneratePackageListTask.DEFAULT_TASK_NAME)
                    })
                    packageListTaskNames
                }
            dependsOn(extraTaskDependencies)

            classpath = project
                .files()
                .from(classpath)
                .from(ProjectUtils.releasedSubprojects(project.rootProject).map { subprojects: List<Project> ->
                    subprojects.map { outputOfProject(it) }
                })
        }

        project.tasks.withType<Javadoc>().configureEach {
            val otherProjectLinks = ArrayList<JavadocOfflineLink>()

            ProjectUtils
                .releasedSubprojects(project.rootProject)
                .get()
                .forEach { projectDependency: Project ->
                    if (projectDependency.path == project.path) {
                        return@forEach
                    }

                    val packageListFilePath: String = projectDependency.tasks
                        .withType<GeneratePackageListTask>()
                        .named(GeneratePackageListTask.DEFAULT_TASK_NAME)
                        .get()
                        .packageListFile
                        .get()
                        .asFile
                        .parentFile
                        .toString()

                    val url = getJTrimUrl(project, projectDependency)
                    otherProjectLinks.add(JavadocOfflineLink(url, packageListFilePath))
                }

            setCommonJavadocConfig(this, toolchainService, otherProjectLinks)
        }
    }

    companion object {
        private fun getJavadocUrl(project: Project): String {
            val version = ProjectUtils
                .tryGetJava(project)
                ?.toolchain
                ?.languageVersion
                ?.orNull
                ?: ProjectUtils.getCompileJavaVersion(project)
            val pattern =
                if (JavaLanguageVersion.of(11) <= version) JAVADOC_11_URL_PATTERN_JDK else JAVADOC_8_URL_PATTERN_JDK
            return pattern.replace("\${version}", version.asInt().toString())
        }

        fun setCommonJavadocConfig(
            task: Javadoc,
            toolchainService: JavaToolchainService,
            extraOfflineLinks: Collection<JavadocOfflineLink>
        ) {

            task.javadocTool.set(ProjectUtils.javadocTool(task.project, toolchainService))

            val config = task.options as StandardJavadocDocletOptions

            // We are catching problems using CheckStyle, and we don't care about undocumented
            // serialization related fields and methods.
            config.addStringOption("Xdoclint:-missing", "-quiet")

            val project = task.project

            val allLinks: MutableList<JavadocOfflineLink> = ArrayList()
            allLinks.addAll(config.linksOffline ?: emptyList())
            allLinks.addAll(extraOfflineLinks)
            allLinks.add(getCommonOfflineLink(project, "java", getJavadocUrl(project)))

            config.linksOffline = allLinks
        }

        private fun getJavadocUrl(project: Project, name: String, defaultUrl: String): String {
            return ProjectUtils.getStringProperty(project, name + "JavadocLink", defaultUrl)!!
        }

        private fun getCommonOfflineLink(project: Project, name: String, defaultUrl: String): JavadocOfflineLink {
            val packageListFile = ProjectUtils.scriptFile(project, "javadoc")
                .resolve(name)
                .toString()

            return JavadocOfflineLink(
                getJavadocUrl(project, name, defaultUrl),
                packageListFile
            )
        }

        private fun getJTrimUrl(project: Project, projectDependency: Project): String {
            if (ReleaseUtils.isRelease(project)) {
                return getExternalJTrimUrl(project, projectDependency, null)
            }

            val versionOverride = ProjectUtils.getStringProperty(project, "externalJTrimJavadocVersion", null)
            if (versionOverride != null && versionOverride.trim().isNotEmpty()) {
                return getExternalJTrimUrl(project, projectDependency, versionOverride)
            }

            return projectDependency
                .tasks
                .withType<Javadoc>()
                .named(JavaPlugin.JAVADOC_TASK_NAME)
                .get()
                .destinationDir
                .let { requireNotNull(it) { "javadoc.destinationDir" } }
                .toURI()
                .toString()
        }

        private fun getExternalJTrimUrl(
            project: Project,
            projectDependency: Project,
            versionOverride: String?
        ): String {
            return getJavadocUrl(project, "java", JAVADOC_JTRIM_URL)
                .replace("\${group}", projectDependency.group.toString())
                .replace("\${name}", projectDependency.name)
                .replace("\${version}", versionOverride ?: Versions.getVersion(projectDependency))
        }

        private fun outputOfProject(project: Project): File {
            return outputOfProjectRef(project).get()
        }

        private fun outputOfProjectRef(project: Project): Provider<File> {
            return project.tasks
                .named(JavaPlugin.JAR_TASK_NAME, Jar::class.java)
                .map { jar: Jar -> jar.archiveFile.get().asFile }
        }

        fun applyJacoco(project: Project) {
            ProjectUtils.applyPlugin(project, "jacoco")
            val jacoco = project.the<JacocoPluginExtension>()
            jacoco.toolVersion = ProjectUtils.getVersion(project, "jacoco")
        }
    }
}
