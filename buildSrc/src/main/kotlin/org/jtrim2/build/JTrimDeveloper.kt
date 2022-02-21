package org.jtrim2.build

import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

class JTrimDeveloper(private val name: String, objects: ObjectFactory) : Named {
    val displayName = objects.property<String>().value(name)
    val email = objects.property<String>()

    override fun getName(): String {
        return name
    }
}
