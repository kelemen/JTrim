package org.jtrim2.build

import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

open class JTrimDevelopment @Inject constructor(objects: ObjectFactory) {
    val url = objects.property<String>()
    val scmUrl = objects.property<String>()
    val developers: NamedDomainObjectContainer<JTrimDeveloper> = objects
            .domainObjectContainer(JTrimDeveloper::class.java) { JTrimDeveloper(it, objects) }
}
