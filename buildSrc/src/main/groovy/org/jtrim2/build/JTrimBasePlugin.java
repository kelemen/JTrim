package org.jtrim2.build;

import java.io.File;
import java.util.Collections;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class JTrimBasePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        try {
            applyUnsafe(project);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void applyUnsafe(Project project) throws Exception {
        ProjectUtils.applyPlugin(project, "base"); // To add "clean" task

        Versions.setVersion(project);
        setupRepositories(project);
    }

    private void setupRepositories(Project project) {
        File repoDefFile = BuildFileUtils.rootPath(project, "gradle", "repositories.gradle").toFile();
        project.apply(Collections.singletonMap("from", repoDefFile));
    }
}
