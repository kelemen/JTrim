package org.jtrim2.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;

public final class JTrimJavaPlugin implements Plugin<Project> {
    private static final String JACOCO_VERSION = "0.7.9";

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

        applyJacoco(project);

        new CheckStyleConfigurer(project, "").configure();
        new MavenConfigurer(project).configure();
    }

    private void configureJava(Project project) {
        ReleaseUtils.setupReleaseTasks(project);
    }

    public static void applyJacoco(Project project) {
        ProjectUtils.applyPlugin(project, "jacoco");
        JacocoPluginExtension jacoco = ProjectUtils.getExtension(project, JacocoPluginExtension.class);
        jacoco.setToolVersion(JACOCO_VERSION);
    }
}
