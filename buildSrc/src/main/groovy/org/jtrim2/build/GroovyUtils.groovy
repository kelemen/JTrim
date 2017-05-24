package org.jtrim2.build

import java.util.function.Supplier
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployment

class GroovyUtils {
    static void configClosure(Object arg, Closure config) {
        config.resolveStrategy = Closure.DELEGATE_FIRST
        config.delegate = arg
        config.call(arg)
    }

    static Closure toSupplierClosure(Supplier<?> supplier) {
        return { supplier.get() }
    }

    static void configureMavenDeployer(Project project) {
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    beforeDeployment { MavenDeployment deployment -> project.signing.signPom(deployment) }
                    pom.project {
                        JTrimProjectInfo projectInfo = ProjectUtils.getProjectInfo(project)

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

