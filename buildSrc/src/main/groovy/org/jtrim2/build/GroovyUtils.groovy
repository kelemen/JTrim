package org.jtrim2.build

import java.util.function.Supplier
import org.gradle.api.Project

class GroovyUtils {
    static Closure toSupplierClosure(Supplier<?> supplier) {
        return { supplier.get() }
    }

    static void setupMavenDeployer(Project project, String jtrimRepoUrl, String repoUser, String repoPassword) {
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    repository(url: jtrimRepoUrl) {
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

