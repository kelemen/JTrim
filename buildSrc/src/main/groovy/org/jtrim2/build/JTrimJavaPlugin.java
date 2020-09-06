package org.jtrim2.build;

import java.io.File;
import java.util.Arrays;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
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
        project.getTasks().withType(Javadoc.class, (task) -> {
            Project rootProject = project.getRootProject();
            JTrimBasePlugin.requireSubprojectsTask(task, rootProject, "jar");

            ConfigurableFileCollection otherProjects = project.files().from(GroovyUtils.toSupplierClosure(() -> {
                return JTrimGroupPlugin.getReleasedSubprojects(rootProject)
                        .map(JTrimJavaPlugin::outputOfProject)
                        .toArray();
            }));
            task.setClasspath(project.files(otherProjects).from(task.getClasspath()));

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
        Jar jar = (Jar)project.getTasks().getByName("jar");
        return jar.getArchiveFile().get().getAsFile();
    }

    public static void applyJacoco(Project project) {
        ProjectUtils.applyPlugin(project, "jacoco");
        JacocoPluginExtension jacoco = ProjectUtils.getExtension(project, JacocoPluginExtension.class);
        jacoco.setToolVersion(ProjectUtils.getVersionStrFor(project, "jacoco"));
    }
}
