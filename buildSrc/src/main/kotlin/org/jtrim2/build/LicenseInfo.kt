package org.jtrim2.build

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

open class LicenseInfo @Inject constructor(objects: ObjectFactory) {
    val name = objects.property<String>()
    val url = objects.property<String>()
}
