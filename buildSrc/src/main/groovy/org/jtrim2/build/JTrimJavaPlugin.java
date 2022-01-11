package org.jtrim2.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.JavadocOfflineLink;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;

public final class JTrimJavaPlugin implements Plugin<Project> {
    public static final String EXTERNAL_JAVA = "java";
    public static final String EXTERNAL_JTRIM2 = "jtrim2";

    private final JavaToolchainService toolchainService;

    @Inject
    public JTrimJavaPlugin(JavaToolchainService toolchainService) {
        this.toolchainService = Objects.requireNonNull(toolchainService, "toolchainService");
    }

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

    public static void setCommonJavadocConfig(
            Javadoc task,
            JavaToolchainService toolchainService,
            ExternalJavadoc... externalJavadocs) {

        task.getJavadocTool().set(ProjectUtils.javadoctool(toolchainService));

        StandardJavadocDocletOptions config = (StandardJavadocDocletOptions) task.getOptions();

        // We are catching problems using CheckStyle, and we don't care about undocumented
        // serialization related fields and methods.
        config.addStringOption("Xdoclint:-missing", "-quiet");

        List<JavadocOfflineLink> offlineLinks = new ArrayList<>();
        for (ExternalJavadoc externalJavadoc: externalJavadocs) {
            offlineLinks.add(externalJavadoc.getOfflineLink(task.getProject()));
        }
        config.setLinksOffline(BuildUtils.withSafeAppended(config.getLinksOffline(), offlineLinks));
    }

    public void configureJavadoc(Project project) {
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

            setCommonJavadocConfig(task, toolchainService, ExternalJavadoc.SELF, ExternalJavadoc.JAVA);
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
