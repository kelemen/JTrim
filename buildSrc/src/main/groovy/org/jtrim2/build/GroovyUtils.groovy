package org.jtrim2.build

import java.util.function.Supplier
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.artifacts.maven.MavenPom

class GroovyUtils {
    static void configClosure(Object arg, Closure config) {
        config.resolveStrategy = Closure.DELEGATE_FIRST
        config.delegate = arg
        config.call(arg)
    }

    static Closure toSupplierClosure(Supplier<?> supplier) {
        return { supplier.get() }
    }

    static void configureMavenPom(Project project, MavenPom pom) {
        pom.project {
            JTrimProjectInfo projectInfo = ProjectUtils.getProjectInfo(project)

            groupId = project.getGroup().toString()
            artifactId = project.getName()
            version = project.getVersion().toString()

            packaging 'jar'
            name projectInfo.displayName
            if (project.description) {
                description project.description
            }

            JTrimDevelopment jtrimDev = ProjectUtils.getDevelopmentInfo(project)

            url jtrimDev.url
            scm {
                connection jtrimDev.scmUrl
                developerConnection jtrimDev.scmUrl
                url jtrimDev.url
            }

            licenses {
                license {
                    LicenseInfo licenseInfo = ProjectUtils.getLicenseInfo(project)
                    name licenseInfo.name
                    url licenseInfo.url
                }
            }

            developers {
                for (JTrimDeveloper dev: jtrimDev.developers.developers) {
                    developer {
                        id dev.id
                        name dev.name
                        email dev.email
                    }
                }
            }
        }
    }

    static void configureMavenDeployer(Project project) {
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    beforeDeployment { MavenDeployment deployment -> project.signing.signPom(deployment) }
                    configureMavenPom(project, pom)
                }
            }
        }
    }

    static void addDeployRepository(Project project, String repoUrl, String repoUser, String repoPassword) {
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    repository(url: repoUrl) {
                        authentication(userName: repoUser, password: repoPassword);
                    }
                }
            }
        }
    }

    private GroovyUtils() {
        throw new AssertionError()
    }
}

