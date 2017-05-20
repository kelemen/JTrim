package org.jtrim2.build

import java.util.function.Supplier
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployment

class GroovyUtils {
    private static final String REPO_URL = 'https://github.com/kelemen/JTrim'
    private static final String REPO_URL_SCM = 'https://github.com/kelemen/JTrim.git'

    static Closure toSupplierClosure(Supplier<?> supplier) {
        return { supplier.get() }
    }

    static void configureMavenDeployer(Project project) {
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    beforeDeployment { MavenDeployment deployment -> project.signing.signPom(deployment) }
                    pom.project {
                        packaging 'jar'
                        name ProjectUtils.getDisplayName(project)
                        if (project.description) {
                            description project.description
                        }

                        url REPO_URL
                        scm {
                            connection "scm:git:${REPO_URL_SCM}"
                            developerConnection "scm:git:${REPO_URL_SCM}"
                            url REPO_URL
                        }

                        licenses {
                            license {
                                name 'GNU LESSER GENERAL PUBLIC LICENSE, Version 3'
                                url 'https://www.gnu.org/licenses/lgpl-3.0.txt'
                            }
                        }

                        developers {
                            developer {
                                id 'kelemen'
                                name 'Attila Kelemen'
                                email 'attila.kelemen85@gmail.com'
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

