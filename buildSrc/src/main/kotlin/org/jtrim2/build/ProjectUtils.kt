package org.jtrim2.build

import java.nio.file.Path
import java.util.Collections
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavadocTool
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.typeOf

object ProjectUtils {
    fun runningWithSupervision(project: Project): Provider<Boolean> {
        return project.providers
            .gradleProperty("runningWithSupervision")
            .map {
                when (it.lowercase()) {
                    "false" -> false
                    "true" -> true
                    else -> throw IllegalArgumentException("Invalid boolean value: $it")
                }
            }
            .orElse(true)
    }

    fun getCompileJavaVersion(project: Project): JavaLanguageVersion {
        return JavaLanguageVersion.of(getVersion(project, "java"))
    }

    fun javadocTool(project: Project, toolchainService: JavaToolchainService): Provider<JavadocTool> {
        return toolchainService.javadocToolFor {
            languageVersion.set(JavaLanguageVersion.of(getVersion(project, "javadocVersion")))
        }
    }

    fun scriptFile(project: Project, vararg subPaths: String): Path {
        val allPaths = ArrayList<String>(subPaths.size + 1)
        allPaths.add("gradle")
        allPaths.addAll(subPaths.asList())
        return project.rootDir.withChildren(allPaths)
    }

    fun applyScript(project: Project, name: String) {
        project.apply(Collections.singletonMap("from", scriptFile(project, name)))
    }

    private fun libs(project: Project): VersionCatalog {
        return project.the<VersionCatalogsExtension>().named("libs")
    }

    fun getVersion(project: Project, name: String): String {
        val version = libs(project)
            .findVersion(name)
            .orElseThrow { NoSuchElementException("Missing version for $name") }

        val requiredVersion = version.requiredVersion
        if (requiredVersion.isNotEmpty()) {
            return requiredVersion
        }

        val strictVersion = version.strictVersion
        return strictVersion.ifEmpty { version.preferredVersion }
    }

    fun getBundle(project: Project, name: String): Provider<ExternalModuleDependencyBundle> {
        return libs(project)
            .findBundle(name)
            .orElseThrow { NoSuchElementException("Missing bundle for $name") }
    }

    fun applyPlugin(project: Project, pluginName: String) {
        project.pluginManager.apply(pluginName)
    }

    inline fun <reified T : Plugin<*>> applyPlugin(project: Project) {
        project.pluginManager.apply(typeOf<T>().concreteClass)
    }

    fun getProjectInfo(project: Project): JTrimProjectInfo {
        return project.extensions.getByType()
    }

    private inline fun <reified T> getRootExtension(project: Project): T {
        return project.rootProject.extensions.getByType()
    }

    fun getLicenseInfo(project: Project): LicenseInfo {
        return getRootExtension(project)
    }

    fun getDevelopmentInfo(project: Project): JTrimDevelopment {
        return getRootExtension(project)
    }

    fun java(project: Project): JavaPluginExtension {
        return tryGetJava(project)
            ?: throw IllegalArgumentException("${project.path} does not have the java plugin applied.")
    }

    fun getModuleName(project: Project): String {
        return "${project.group}.${project.name.replace("jtrim-", "")}"
    }

    fun tryGetJava(project: Project): JavaPluginExtension? {
        return project.extensions.findByType(JavaPluginExtension::class.java)
    }

    fun getStringProperty(project: Project, name: String, defaultValue: String?): String? {
        if (!project.hasProperty(name)) {
            return defaultValue
        }

        val result = project.property(name) ?: return defaultValue
        val resultStr = result.toString()
        return resultStr.trim()
    }

    fun isReleasedProject(project: Project): Boolean {
        return project.plugins.findPlugin(JTrimJavaPlugin::class.java) != null
    }

    fun releasedSubprojects(parent: Project): Provider<List<Project>> {
        return releasedProjects(parent.objects, parent.providers) { parent.subprojects.asSequence() }
    }

    fun releasedProjects(
        objects: ObjectFactory,
        providers: ProviderFactory,
        projectsProviders: () -> Sequence<Project>
    ): Provider<List<Project>> {

        val result = objects.listProperty<Project>()
        result.set(providers.provider {
            projectsProviders()
                .filter { isReleasedProject(it) }
                .toList()
        })
        return result
    }
}
