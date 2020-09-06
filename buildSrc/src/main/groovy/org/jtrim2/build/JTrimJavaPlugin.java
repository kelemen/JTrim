package org.jtrim2.build;

import java.io.File;
import java.util.Arrays;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;

public final class JTrimJavaPlugin implements Plugin<Project> {
    public static final String EXTERNAL_JAVA = "java";
    public static final String EXTERNAL_JTRIM2 = "jtrim2";

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

        configureJavadoc(project);
    }

    private void configureJava(Project project) {
        ReleaseUtils.setupReleaseTasks(project);
    }

    public static void configureJavadoc(Project project) {
        project.getTasks().withType(Javadoc.class).configureEach(task -> {
            Project rootProject = project.getRootProject();
            JTrimBasePlugin.requireSubprojectsTask(task, rootProject, "jar");

            task.setClasspath(project
                    .files()
                    .from(task.getClasspath())
                    .from(ProjectUtils.releasedSubprojects(rootProject)
                    .map(subprojects -> {
                        return BuildUtils.mapToReadOnly(subprojects, JTrimJavaPlugin::outputOfProject);
                    }))
            );

            StandardJavadocDocletOptions config = (StandardJavadocDocletOptions) task.getOptions();
            config.setLinksOffline(
                    BuildUtils.withSafeAppended(config.getLinksOffline(), Arrays.asList(
                            ExternalJavadoc.SELF.getOfflineLink(project),
                            ExternalJavadoc.JAVA.getOfflineLink(project)
                    ))
            );
        });
    }

    private static File outputOfProject(Project project) {
        return outputOfProjectRef(project).get();
    }

    private static Provider<File> outputOfProjectRef(Project project) {
        return project.getTasks()
                .named(JavaPlugin.JAR_TASK_NAME, Jar.class)
                .map(jar -> jar.getArchiveFile().get().getAsFile());
    }

    public static void applyJacoco(Project project) {
        ProjectUtils.applyPlugin(project, "jacoco");
        JacocoPluginExtension jacoco = ProjectUtils.getExtension(project, JacocoPluginExtension.class);
        jacoco.setToolVersion(ProjectUtils.getVersionStrFor(project, "jacoco"));
    }
}
