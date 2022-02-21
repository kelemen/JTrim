package org.jtrim2.build

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

class JTrimProjectInfo(project: Project) {
    val displayName: Property<String>
    val description: Property<String>

    init {
        val objects = project.objects
        displayName = objects.property()
        displayName.set(project.name)

        description = objects.property()
        description.set(displayName.map { project.description ?: it })
    }
}
