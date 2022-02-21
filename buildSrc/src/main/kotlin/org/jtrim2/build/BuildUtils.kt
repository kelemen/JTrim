package org.jtrim2.build

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named

fun Path.withChildren(children: List<String>): Path = children.fold(this) { parent, child -> parent.resolve(child) }

fun Path.withChildren(vararg children: String): Path = withChildren(children.asList())

fun File.withChildren(children: List<String>): Path = toPath().withChildren(children)

fun File.withChildren(vararg children: String): Path = toPath().withChildren(*children)

fun Path.readTextFile(): String {
    return readTextFile(StandardCharsets.UTF_8)
}

fun Path.readTextFile(charset: Charset): String {
    val bytes = Files.readAllBytes(this)
    return String(bytes, charset)
}

inline fun <reified T : Task> TaskContainer.tryGetTaskRef(name: String): TaskProvider<T>? {
    return try {
        named<T>(name)
    } catch (ex: UnknownTaskException) {
        null
    }
}
