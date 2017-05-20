package org.jtrim2.build;

import java.io.IOException;
import org.gradle.api.Project;

import static org.jtrim2.build.BuildFileUtils.*;

public final class Versions {
    private static final String GROUP_NAME = "org.jtrim2";

    private static final String VERSION_BASE_PROPERTY = "versionBase";

    public static void setVersion(Project project) throws IOException {
        String suffix = ReleaseUtils.isRelease(project) ? "" : "-SNAPSHOT";

        String versionBase = readTextFile(rootPath(project, "version.txt")).trim();
        project.getExtensions().add(VERSION_BASE_PROPERTY, versionBase);

        project.setGroup(GROUP_NAME);
        project.setVersion(versionBase + suffix);
    }

    public static String getVersionBase(Project project) {
        return ProjectUtils.getStringProperty(project, VERSION_BASE_PROPERTY, "");
    }

    private Versions() {
        throw new AssertionError();
    }
}
