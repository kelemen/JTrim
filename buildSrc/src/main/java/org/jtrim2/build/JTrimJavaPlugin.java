package org.jtrim2.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.JavadocOfflineLink;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;

public final class JTrimJavaPlugin implements Plugin<Project> {
    private static final String JAVADOC_URL_JDK = "https://docs.oracle.com/javase/8/docs/api/";
    private static final String JAVADOC_JTRIM_URL = "https://www.javadoc.io/doc/${group}/${name}/${version}/";

    private final JavaToolchainService toolchainService;

    @Inject
    public JTrimJavaPlugin(JavaToolchainService toolchainService) {
        this.toolchainService = Objects.requireNonNull(toolchainService, "toolchainService");
    }

    @Override
    public void apply(Project project) {
        ProjectUtils.applyPlugin(project, JTrimJavaBasePlugin.class);

        configureJava(project);

        ProjectUtils.getExtension(project, JavaPluginExtension.class).manifest(manifest -> {
            manifest.attributes(Collections.singletonMap("Automatic-Module-Name", ProjectUtils.getModuleName(project)));
        });

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
            Collection<? extends JavadocOfflineLink> extraOfflineLinks) {

        task.getJavadocTool().set(ProjectUtils.javadocTool(toolchainService));

        StandardJavadocDocletOptions config = (StandardJavadocDocletOptions) task.getOptions();

        // We are catching problems using CheckStyle, and we don't care about undocumented
        // serialization related fields and methods.
        config.addStringOption("Xdoclint:-missing", "-quiet");

        Project project = task.getProject();

        List<JavadocOfflineLink> allLinks = new ArrayList<>();
        allLinks.addAll(BuildUtils.emptyIfNull(config.getLinksOffline()));
        allLinks.addAll(extraOfflineLinks);
        allLinks.add(getCommonOfflineLink(project, "java", JAVADOC_URL_JDK));

        config.setLinksOffline(allLinks);
    }

    private static String getJavadocUrl(Project project, String name, String defaultUrl) {
        return ProjectUtils.getStringProperty(project, name + "JavadocLink", defaultUrl);
    }

    private static JavadocOfflineLink getCommonOfflineLink(Project project, String name, String defaultUrl) {
        String packageListFile = ProjectUtils.scriptFile(project, "javadoc")
                .resolve(name)
                .toString();

        return new JavadocOfflineLink(
                getJavadocUrl(project, name, defaultUrl),
                packageListFile
        );
    }

    public void configureJavadoc(Project project) {
        project.getTasks().register(GeneratePackageListTask.DEFAULT_TASK_NAME, GeneratePackageListTask.class);

        project.getTasks().withType(Javadoc.class).configureEach(task -> {
            Provider<List<String>> extraTaskDependencies = ProjectUtils
                    .releasedSubprojects(project.getRootProject())
                    .map(projects -> {
                        List<String> packageListTaskNames = new ArrayList<>();
                        projects.forEach(projectDependency -> {
                            packageListTaskNames.add(projectDependency.getPath() + ":" + JavaPlugin.JAR_TASK_NAME);
                            packageListTaskNames.add(projectDependency.getPath() + ":" + GeneratePackageListTask.DEFAULT_TASK_NAME);
                        });
                        return packageListTaskNames;
                    });
            task.dependsOn(extraTaskDependencies);

            task.setClasspath(project
                    .files()
                    .from(task.getClasspath())
                    .from(ProjectUtils.releasedSubprojects(project.getRootProject())
                            .map(subprojects -> {
                                return BuildUtils.mapToReadOnly(subprojects, JTrimJavaPlugin::outputOfProject);
                            }))
            );
        });

        project.getGradle().projectsEvaluated(g -> {
            project.getTasks().withType(Javadoc.class).configureEach(task -> {
                List<JavadocOfflineLink> otherProjectLinks = new ArrayList<>();

                ProjectUtils
                        .releasedSubprojects(project.getRootProject())
                        .get()
                        .forEach(projectDependency -> {
                            if (projectDependency.getPath().equals(project.getPath())) {
                                return;
                            }

                            String packageListFilePath = projectDependency.getTasks()
                                    .withType(GeneratePackageListTask.class)
                                    .named(GeneratePackageListTask.DEFAULT_TASK_NAME)
                                    .get()
                                    .getPackageListFile()
                                    .get()
                                    .getAsFile()
                                    .getParentFile()
                                    .toString();

                            String url = getJTrimUrl(project, projectDependency);
                            otherProjectLinks.add(new JavadocOfflineLink(url, packageListFilePath));
                        });

                setCommonJavadocConfig(task, toolchainService, otherProjectLinks);
            });
        });
    }

    private static String getJTrimUrl(Project project, Project projectDependency) {
        if (ReleaseUtils.isRelease(project)) {
            return getExternalJTrimUrl(project, projectDependency, null);
        }

        String versionOverride = ProjectUtils.getStringProperty(project, "externalJTrimJavadocVersion", null);
        if (versionOverride != null && !versionOverride.trim().isEmpty()) {
            return getExternalJTrimUrl(project, projectDependency, versionOverride);
        }

        File destDir = projectDependency
                .getTasks()
                .withType(Javadoc.class)
                .named(JavaPlugin.JAVADOC_TASK_NAME)
                .get()
                .getDestinationDir();
        Objects.requireNonNull(destDir, "javadoc.destinationDir");
        return destDir.toURI().toString();
    }

    private static String getExternalJTrimUrl(Project project, Project projectDependency, String versionOverride) {
        return getJavadocUrl(project, "java", JAVADOC_JTRIM_URL)
                .replace("${group}", projectDependency.getGroup().toString())
                .replace("${name}", projectDependency.getName())
                .replace("${version}", versionOverride != null
                        ? versionOverride
                        : Versions.getVersion(projectDependency)
                );
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
        jacoco.setToolVersion(ProjectUtils.getVersion(project, "jacoco"));
    }
}
