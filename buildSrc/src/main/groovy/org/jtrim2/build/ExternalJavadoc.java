package org.jtrim2.build;

import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.external.javadoc.JavadocOfflineLink;

public enum ExternalJavadoc {
    SELF("jtrim2", "https://htmlpreview.github.io/?https://raw.githubusercontent.com/kelemen/api-docs/blob/jtrim/api/"),
    JAVA("java", "https://docs.oracle.com/javase/8/docs/api/");

    private final String name;
    private final String defaultLink;

    private ExternalJavadoc(String name, String defaultLink) {
        this.name = name;
        this.defaultLink = defaultLink;
    }

    public String getName() {
        return name;
    }

    public String getDefaultLink() {
        return defaultLink;
    }

    public Path getPackageListDir(Project project) {
        return javadocConfigDir(project).resolve(name);
    }

    public Path getPackageListFile(Project project) {
        return getPackageListDir(project).resolve("package-list");
    }

    public JavadocOfflineLink getOfflineLink(Project project) {
        Path packageList = getPackageListDir(project);
        return new JavadocOfflineLink(defaultLink, packageList.toString());
    }

    private static Path javadocConfigDir(Project project) {
        return ProjectUtils.scriptFile(project, "javadoc");
    }
}
