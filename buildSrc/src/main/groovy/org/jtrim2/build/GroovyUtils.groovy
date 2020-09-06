package org.jtrim2.build

import java.util.function.Supplier
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.artifacts.maven.MavenPom

class GroovyUtils {
    static <T> void configClosure(T arg, @DelegatesTo(genericTypeIndex = 0) Closure<?> config) {
        config.resolveStrategy = Closure.DELEGATE_FIRST
        config.delegate = arg
        config.call(arg)
    }

    static <T> Closure<T> toSupplierClosure(Supplier<? extends T> supplier) {
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
            } else {
                description projectInfo.displayName
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

    static void configureMavenDeployer(Task uploadTask) {
        Project project = uploadTask.project
        uploadTask.repositories {
            mavenDeployer {
                beforeDeployment { MavenDeployment deployment -> project.signing.signPom(deployment) }
                configureMavenPom(project, pom)
            }
        }
    }

    static void addDeployRepository(Task uploadTask, String repoUrl, String repoUser, String repoPassword) {
        uploadTask.repositories {
            mavenDeployer {
                repository(url: repoUrl) {
                    authentication(userName: repoUser, password: repoPassword);
                }
            }
        }
    }

    private GroovyUtils() {
        throw new AssertionError()
    }
}

