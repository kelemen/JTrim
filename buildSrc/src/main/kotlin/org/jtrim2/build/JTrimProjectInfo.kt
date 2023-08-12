package org.jtrim2.build

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

class JTrimProjectInfo(project: Project) {
    val displayName: Property<String> = project.objects.property()
    val description: Property<String> = project.objects.property()

    init {
        displayName.set(project.name)
        description.set(displayName.map { project.description ?: it })
    }
}
