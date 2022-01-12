package org.jtrim2.build;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.reporting.DirectoryReport;
import org.gradle.api.reporting.Report;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.gradle.testing.jacoco.tasks.JacocoReportsContainer;

public final class JTrimGroupPlugin implements Plugin<Project> {
    private final JavaToolchainService toolchainService;

    @Inject
    public JTrimGroupPlugin(JavaToolchainService toolchainService) {
        this.toolchainService = Objects.requireNonNull(toolchainService, "toolchainService");
    }

    @Override
    public void apply(Project project) {
        try {
            applyUnsafe(project);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void applyUnsafe(Project project) throws Exception {
        ProjectUtils.applyPlugin(project, JTrimBasePlugin.class);

        ReleaseUtils.setupMainReleaseTask(project);

        setupJavadoc(project);
        setupJacoco(project);
    }

    private static SourceSetContainer sourceSets(Project project) {
        JavaPluginExtension java = ProjectUtils.java(project);
        if (java == null) {
            return null;
        }
        return java.getSourceSets();
    }

    private static SourceSet sourceSet(Project project, String sourceSetName) {
        SourceSetContainer sourceSet = sourceSets(project);
        if (sourceSet == null) {
            return null;
        }

        return sourceSet.getByName(sourceSetName);
    }

    private static Collection<File> sourceDirs(Project project, String sourceSetName) {
        SourceSet sourceSet = sourceSet(project, sourceSetName);
        if (sourceSet == null) {
            return null;
        }
        return sourceSet.getAllSource().getSrcDirs();
    }

    private static Configuration projectsOfConfig(Project project, String configName) {
        return project.getConfigurations()
                .getByName(configName)
                .copyRecursive((Dependency dependency) -> dependency instanceof ProjectDependency);
    }

    private void setupJavadoc(Project project) {
        setupJavadoc(project, ProjectUtils.releasedSubprojects(project));
    }

    private void setupJavadoc(Project project, Provider<List<Project>> subprojectsRef) {
        TaskContainer tasks = project.getTasks();
        TaskProvider<Javadoc> javadocRef = tasks.register(JavaPlugin.JAVADOC_TASK_NAME, Javadoc.class, task -> {
            task.setTitle("JTrim " + Versions.getVersion(project) + " - All modules");
            task.setDestinationDir(new File(project.getBuildDir(), "merged-javadoc"));

            task.source(subprojectsRef.map(subprojects -> {
                return BuildUtils.flatMapToReadOnly(subprojects, subproject -> {
                    return sourceDirs(subproject, SourceSet.MAIN_SOURCE_SET_NAME).stream();
                });
            }));

            task.dependsOn(subprojectsRef.map(projects -> {
                return BuildUtils.mapToReadOnly(projects, p -> p.getPath() + ":" + JavaPlugin.JAR_TASK_NAME);
            }));

            task.setClasspath(project.getObjects()
                    .fileCollection()
                    .from(task.getClasspath())
                    .from(subprojectsRef.map(subprojects -> {
                        return BuildUtils.flatMapToReadOnly(subprojects, subproject -> {
                            return projectsOfConfig(subproject, JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
                                    .resolve()
                                    .stream();
                        });
                    }))
            );

            JTrimJavaPlugin.setCommonJavadocConfig(task, toolchainService, Collections.emptyList());

            JTrimBasePlugin.requireEvaluateSubprojects(task);
        });
        tasks.named(LifecycleBasePlugin.BUILD_TASK_NAME).configure(build -> build.dependsOn(javadocRef));

        TaskProvider<CheckUniquePackagesTask> checkUniquePackages = tasks.register(
                "checkUniquePackages",
                CheckUniquePackagesTask.class,
                task -> {
                    task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
                }
        );
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure(check -> check.dependsOn(checkUniquePackages));
    }

    private void setupJacoco(Project project) {
        setupJacoco(project, ProjectUtils.releasedSubprojects(project));
    }

    private void setupJacoco(Project project, Provider<List<Project>> subprojectsRef) {
        JTrimJavaPlugin.applyJacoco(project);

        Provider<List<SourceSet>> mainSourceSets = subprojectsRef.map(subprojects -> {
            return BuildUtils.mapToReadOnly(subprojects, subproject -> {
                return sourceSet(subproject, SourceSet.MAIN_SOURCE_SET_NAME);
            });
        });

        project.getTasks().register("jacocoTestReport", JacocoReport.class).configure(jacocoReport -> {
            JTrimBasePlugin.requireEvaluateSubprojects(jacocoReport);

            jacocoReport.getSourceDirectories().from(mainSourceSets.map(sourceSets -> {
                return BuildUtils.flatMapToReadOnly(sourceSets, sourceSet -> {
                    return sourceSet.getAllSource().getSrcDirs().stream();
                });
            }));

            jacocoReport.getClassDirectories().from(mainSourceSets.map(sourceSets -> {
                return BuildUtils.mapToReadOnly(sourceSets, sourceSet -> {
                    return sourceSet.getOutput();
                });
            }));

            jacocoReport.getExecutionData().from(subprojectsRef.map(subprojects -> {
                return BuildUtils.flatMapToReadOnly(subprojects, subproject -> {
                    TaskProvider<JacocoReport> taskRef = BuildUtils.tryGetTaskRef(
                            subproject.getTasks(),
                            "jacocoTestReport",
                            JacocoReport.class
                    );
                    if (taskRef == null) {
                        return Stream.empty();
                    }

                    return taskRef.get()
                            .getExecutionData()
                            .getFiles()
                            .stream()
                            .filter(File::exists);
                });
            }));

            List<ReportDef<?>> reportDefs = Arrays.asList(
                    new ReportDef<>(JacocoReportsContainer::getHtml, DirectoryReport::getEntryPoint, true),
                    new ReportDef<>(JacocoReportsContainer::getXml, r -> r.getOutputLocation().get().getAsFile(), false),
                    new ReportDef<>(JacocoReportsContainer::getCsv, r -> r.getOutputLocation().get().getAsFile(), false)
            );

            jacocoReport.reports(reportsContainer -> {
                reportDefs.forEach(reportDef -> {
                    reportDef.getReport(reportsContainer).getRequired().set(reportDef.isDefaultRequired());
                });
            });

            jacocoReport.doLast(task -> {
                reportDefs.forEach(reportDef -> {
                    URI reportUri = reportDef.getTarget(jacocoReport.getReports()).toURI();
                    System.out.println("Successfully generated Jacoco report to " + reportUri);
                });
            });
        });
    }

    private static final class ReportDef<R extends Report> {
        private final Function<JacocoReportsContainer, R> reportProvider;
        private final Function<R, File> targetProvider;
        private final boolean defaultRequired;

        public ReportDef(
                Function<JacocoReportsContainer, R> reportProvider,
                Function<R, File> targetProvider,
                boolean defaultRequired) {

            this.reportProvider = Objects.requireNonNull(reportProvider, "reportProvider");
            this.targetProvider = Objects.requireNonNull(targetProvider, "targetProvider");
            this.defaultRequired = defaultRequired;
        }

        public boolean isDefaultRequired() {
            return defaultRequired;
        }

        public R getReport(JacocoReportsContainer reportsContainer) {
            return reportProvider.apply(reportsContainer);
        }

        public File getTarget(JacocoReportsContainer reportsContainer) {
            return targetProvider.apply(getReport(reportsContainer));
        }
    }
}
