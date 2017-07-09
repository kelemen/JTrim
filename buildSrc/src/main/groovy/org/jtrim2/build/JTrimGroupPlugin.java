package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static Stream<Project> getReleasedSubprojects(Project parent) {
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

        tasks.create("checkUniquePackages", task -> {
            tasks.getByName("check").dependsOn(task);
            task.doLast(actionTask -> verifyNoConflictingPackages(project));
        });

        project.getTasks().create("generatePackageList", (task) -> {
            JTrimBasePlugin.requireEvaluateSubprojects(task);
            task.doLast((t) -> {
                try {
                    generatePackageList(project, ExternalJavadoc.SELF.getPackageListFile(project));
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        });
    }

    private static Set<String> findAllPackagesOfAllProjects(Project rootProject) throws IOException {
        Set<String> packages = new HashSet<>();
        for (Project subProject: rootProject.getSubprojects()) {
            collectPackageListFromSources(subProject, packages);
        }
        return packages;
    }

    private static void generatePackageList(Project rootProject, Path packageList) throws IOException {
        Set<String> packages = findAllPackagesOfAllProjects(rootProject);

        List<String> sortedPackages = new ArrayList<>(packages);
        sortedPackages.sort(String::compareTo);
        sortedPackages.add("");

        byte[] outputContent = String.join("\n", sortedPackages).getBytes(StandardCharsets.UTF_8);
        Files.write(packageList, outputContent);
    }

    private static Set<String> findAllPackagesOfProject(Project project) throws IOException {
        Set<String> packages = new HashSet<>();
        collectPackageListFromSources(project, packages);
        return packages;
    }

    private static void verifyNoConflictingPackages(Project rootProject) {
        try {
            verifyNoConflictingPackagesUnsafe(rootProject);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void verifyNoConflictingPackagesUnsafe(Project rootProject) throws IOException {
        Map<String, String> allPackages = new HashMap<>();
        for (Project subProject: rootProject.getSubprojects()) {
            Set<String> packagesOfProject = findAllPackagesOfProject(subProject);
            String projectName = subProject.getPath();
            packagesOfProject.forEach(pckg -> {
                String prevOwner = allPackages.putIfAbsent(pckg, projectName);
                if (prevOwner != null) {
                    throw new IllegalStateException("Package \"" + pckg + "\" was found in multiple projects: "
                            + prevOwner + ", " + projectName);
                }
            });
        }
    }

    private static void collectPackageListFromSources(Project project, Set<String> result) throws IOException {
        if (!project.getPlugins().hasPlugin(JTrimJavaPlugin.class)) {
            return;
        }

        JavaPluginConvention java = ProjectUtils.java(project);
        SourceSet sourceSet = java.getSourceSets().getByName("main");
        for (File sourceRoot: sourceSet.getAllJava().getSrcDirs()) {
            collectPackageListFromSourceRoot(sourceRoot.toPath(), result);
        }
    }

    private static void collectPackageListFromSourceRoot(Path sourceRoot, Set<String> result) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return;
        }

        FileCounter rootCounter = new FileCounter(sourceRoot);
        Files.walkFileTree(sourceRoot, new FileVisitor<Path>() {
            private final Deque<FileCounter> counters = new ArrayDeque<>(Collections.singleton(rootCounter));
            private FileCounter topCounter = rootCounter;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                topCounter = new FileCounter(dir);
                counters.push(topCounter);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                topCounter.fileCount++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (topCounter.fileCount > 0) {
                    result.add(toPackageName(sourceRoot.relativize(dir)));
                }
                counters.pop();
                topCounter = counters.peekFirst();
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String toPackageName(Path relPath) {
        StringBuilder packageName = new StringBuilder();
        for (Path name: relPath) {
            if (packageName.length() > 0) {
                packageName.append('.');
            }
            packageName.append(name.toString());
        }
        return packageName.toString();
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

    private static final class FileCounter {
        public int fileCount = 0;
        public final Path dir;

        public FileCounter(Path dir) {
            this.dir = dir;
        }
    }
}
