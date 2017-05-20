package org.jtrim2.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class JTrimJavaPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        ProjectUtils.applyPlugin(project, JTrimJavaBasePlugin.class);
        try {
            applyUnsafe(project);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void applyUnsafe(Project project) throws Exception {
        configureJava(project);

        ProjectUtils.applyPlugin(project, "jacoco");

        new CheckStyleConfigurer(project, "").configure();
        new MavenConfigurer(project).configure();
    }

    private void configureJava(Project project) {
        ReleaseUtils.setupReleaseTasks(project);
    }
}
