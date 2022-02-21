package org.jtrim2.build

import org.gradle.api.Project

object Versions {
    private const val GROUP_NAME = "org.jtrim2"
    private const val VERSION_BASE_PROPERTY = "versionBase"
    private const val VERSION_SUFFIX_PROPERTY = "versionSuffix"

    fun setVersion(project: Project) {
        val versionBase = project.rootDir.withChildren("version.txt").readTextFile().trim()
        project.extensions.add(VERSION_BASE_PROPERTY, versionBase)
        project.group = GROUP_NAME
        project.version = versionBase + getVersionSuffix(project)
    }

    private fun getVersionSuffix(project: Project): String {
        val release = ReleaseUtils.isRelease(project)
        val defaultSuffix = if (release) "" else "DEV"
        val suffix = ProjectUtils.getStringProperty(project, VERSION_SUFFIX_PROPERTY, defaultSuffix)!!
        return if (suffix.isEmpty()) "" else "-$suffix"
    }

    fun getVersion(project: Project): String {
        return project.version.toString()
    }

    fun getVersionBase(project: Project): String {
        return ProjectUtils.getStringProperty(project, VERSION_BASE_PROPERTY, "")!!
    }
}
