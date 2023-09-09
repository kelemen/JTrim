package org.jtrim2.build

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

open class DownloadFileTask @Inject constructor(
    objects: ObjectFactory,
) : DefaultTask() {
    @Input
    val sourceUrl: Property<String> = objects.property<String>()

    @OutputFile
    val destinationFile: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun downloadFile() {
        val uri = URI(sourceUrl.get())
        val request = HttpRequest
            .newBuilder(uri)
            .GET()
            .build()

        val destPath = destinationFile.get().asFile.toPath().toAbsolutePath()
        val tmpDownloadPath = temporaryDir.toPath().resolve(destPath.fileName)

        val bodyHandler = HttpResponse.BodyHandlers.ofFile(tmpDownloadPath)

        val response = HttpClient
            .newHttpClient()
            .send(request, bodyHandler)
        if ((response.statusCode() / 100) != 2) {
            throw IOException("Failed to download file (${response.statusCode()}): $uri")
        }

        Files.createDirectories(destPath.parent)
        Files.move(tmpDownloadPath, destPath, StandardCopyOption.REPLACE_EXISTING)
    }
}
