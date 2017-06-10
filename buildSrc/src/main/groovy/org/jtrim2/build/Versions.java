package org.jtrim2.build;

import java.io.IOException;
import org.gradle.api.Project;

import static org.jtrim2.build.BuildFileUtils.*;

public final class Versions {
    private static final String GROUP_NAME = "org.jtrim2";

    private static final String VERSION_BASE_PROPERTY = "versionBase";
    private static final String VERSION_SUFFIX_PROPERTY = "versionSuffix";

    public static void setVersion(Project project) throws IOException {
        String versionBase = readTextFile(rootPath(project, "version.txt")).trim();
        project.getExtensions().add(VERSION_BASE_PROPERTY, versionBase);

        project.setGroup(GROUP_NAME);
        project.setVersion(versionBase + getVersionSuffix(project));
    }

    private static String getVersionSuffix(Project project) {
        boolean release = ReleaseUtils.isRelease(project);

        String defaultSuffix = release ? "" : "SNAPSHOT";
        String suffix = ProjectUtils.getStringProperty(project, VERSION_SUFFIX_PROPERTY, defaultSuffix);
        return suffix.isEmpty()
                ? ""
                : "-" + suffix;
    }

    public static String getVersion(Project project) {
        Object version = project.getVersion();
        return version != null ? version.toString() : null;
    }

    public static String getVersionBase(Project project) {
        return ProjectUtils.getStringProperty(project, VERSION_BASE_PROPERTY, "");
    }

    private Versions() {
        throw new AssertionError();
    }
}
