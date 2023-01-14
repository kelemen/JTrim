package org.jtrim2.build

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension
import org.jtrim2.build.ProjectUtils.getDevelopmentInfo
import org.jtrim2.build.ProjectUtils.getLicenseInfo
import org.jtrim2.build.ProjectUtils.getProjectInfo

class MavenConfigurer(private val project: Project) {
    fun configure() {
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")

        val publishing = project.the<PublishingExtension>()
        val signing = project.the<SigningExtension>()

        publishing.publications {
            val mainPublication = create<MavenPublication>("main") { configureMainPublication(this) }
            signing.sign(mainPublication)
        }

        publishing.repositories { mavenCentral { configureCentral(this) } }
    }

    private fun configureCentral(repo: MavenArtifactRepository) {
        repo.setUrl(
            ProjectUtils.getStringProperty(
                project,
                "publishCentralRepoUrl",
                "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            )!!
        )
        repo.credentials {
            username = ProjectUtils.getStringProperty(
                project,
                "publishCentralUserName",
                ""
            )!!
            password = ProjectUtils.getStringProperty(
                project,
                "publishCentralPassword",
                ""
            )!!
        }
    }

    private fun configureMainPublication(publication: MavenPublication) {
        publication.from(project.components.getByName("java"))
        publication.pom {
            val projectInfo = getProjectInfo(project)
            val jtrimDev = getDevelopmentInfo(project)

            packaging = "jar"
            name.set(projectInfo.displayName)
            description.set(projectInfo.description)

            url.set(jtrimDev.url)
            scm {
                connection.set(jtrimDev.scmUrl)
                developerConnection.set(jtrimDev.scmUrl)
                url.set(jtrimDev.url)
            }

            licenses {
                license {
                    val licenseInfo = getLicenseInfo(project)
                    name.set(licenseInfo.name)
                    url.set(licenseInfo.url)
                }
            }

            val pom = this
            jtrimDev.developers.all { addDeveloper(pom, this) }
            jtrimDev.developers.whenObjectRemoved { throw IllegalStateException("Cannot handle removal of developer.") }
        }
    }

    private fun addDeveloper(pom: MavenPom, dev: JTrimDeveloper) {
        pom.developers {
            developer {
                id.set(dev.name)
                name.set(dev.displayName)
                email.set(dev.email)
            }
        }
    }
}
