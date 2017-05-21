package org.jtrim2.build;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.testing.jacoco.tasks.JacocoReport;

public final class JTrimGroupPlugin implements Plugin<Project> {
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

    private static Stream<Project> getReleasedSubprojects(Project parent) {
        return parent.getSubprojects().stream().filter(ProjectUtils::isReleasedProject);
    }

    private static SourceSetContainer sourceSets(Project project) {
        JavaPluginConvention java = ProjectUtils.java(project);
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
        TaskContainer tasks = project.getTasks();
        tasks.create("javadoc", Javadoc.class, (task) -> {
            task.setTitle("JTrim " + Versions.getVersion(project) + " - All modules");
            task.setDestinationDir(new File(project.getBuildDir(), "merged-javadoc"));
            task.source(GroovyUtils.toSupplierClosure(() -> {
                return getReleasedSubprojects(project)
                        .flatMap((subproject) -> sourceDirs(subproject, "main").stream())
                        .toArray();
            }));

            ConfigurableFileCollection classpath = (ConfigurableFileCollection)task.getClasspath();
            classpath.from(GroovyUtils.toSupplierClosure(() -> {
                return getReleasedSubprojects(project)
                        .flatMap((subproject) -> projectsOfConfig(subproject, "compile").resolve().stream())
                        .toArray();
            }));

            JTrimBasePlugin.requireEvaluateSubprojects(task);
        });
    }

    private void setupJacoco(Project project) {
        JTrimJavaPlugin.applyJacoco(project);

        JacocoReport jacocoReport = project.getTasks().create("jacocoTestReport", JacocoReport.class);
        JTrimBasePlugin.requireEvaluateSubprojects(jacocoReport);

        BuildUtils.lazilyConfiguredTask(jacocoReport, (task) -> {
            List<Project> subprojects = getReleasedSubprojects(project).collect(Collectors.toList());

            List<SourceSet> mainSourceSets = subprojects.stream()
                    .map((subproject) -> sourceSet(subproject, "main"))
                    .collect(Collectors.toList());

            jacocoReport.setSourceDirectories(project.files(mainSourceSets.stream()
                    .flatMap((sourceSet) -> sourceSet.getAllSource().getSrcDirs().stream())
                    .toArray()));
            jacocoReport.setClassDirectories(project.files(mainSourceSets.stream()
                    .map((sourceSet) -> sourceSet.getOutput())
                    .toArray()));

            Object[] allSubExecData = subprojects.stream()
                    .map((subproject) -> subproject.getTasks().findByName("jacocoTestReport"))
                    .filter((subReportTask) -> subReportTask != null)
                    .map((subReportTask) -> ((JacocoReport)subReportTask).getExecutionData())
                    .filter((subExecData) -> !subExecData.isEmpty())
                    .toArray();
            jacocoReport.setExecutionData(project.files(allSubExecData));

            jacocoReport.reports((reportsContainer) -> {
                reportsContainer.getHtml().setEnabled(true);
                reportsContainer.getXml().setEnabled(false);
                reportsContainer.getCsv().setEnabled(false);
            });
        });

        jacocoReport.doLast((task) -> {
            URI reportUri = jacocoReport.getReports().getHtml().getEntryPoint().toURI();
            System.out.println("Successfully generated report to " + reportUri);
        });
    }
}
