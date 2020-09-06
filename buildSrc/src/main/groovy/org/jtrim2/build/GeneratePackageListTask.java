package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

public class GeneratePackageListTask extends DefaultTask {
    private final RegularFileProperty packageListFile;

    @Inject
    public GeneratePackageListTask(ProjectLayout layout, ObjectFactory objects, ProviderFactory providers) {
        getOutputs().upToDateWhen(t -> false);

        this.packageListFile = objects.fileProperty();
        this.packageListFile.set(layout.file(providers.provider(() -> {
            return ExternalJavadoc.SELF.getPackageListFile(getProject()).toFile();
        })));
    }

    @OutputFile
    public RegularFileProperty getPackageListFile() {
        return packageListFile;
    }

    @TaskAction
    public void generatePackageList() throws IOException {
        // FIXME: Accessing the project instance will interfere with the configuration cache in the future.
        Project rootProject = getProject();

        Set<String> packages = findAllPackagesOfAllProjects(rootProject);

        List<String> sortedPackages = new ArrayList<>(packages);
        sortedPackages.sort(String::compareTo);
        sortedPackages.add("");

        byte[] outputContent = String.join("\n", sortedPackages).getBytes(StandardCharsets.UTF_8);
        Files.write(packageListFile.get().getAsFile().toPath(), outputContent);
    }

    private static Set<String> findAllPackagesOfAllProjects(Project rootProject) throws IOException {
        Set<String> packages = new HashSet<>();
        for (Project subProject: rootProject.getSubprojects()) {
            collectPackageListFromSources(subProject, packages);
        }
        return packages;
    }

    private static void collectPackageListFromSources(Project project, Set<String> result) throws IOException {
        if (!ProjectUtils.isReleasedProject(project)) {
            return;
        }

        JavaPluginConvention java = ProjectUtils.tryGetJava(project);
        if (java == null) {
            return;
        }

        SourceSet sourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        for (File sourceRoot: sourceSet.getAllJava().getSrcDirs()) {
            PackageUtils.collectPackageListFromSourceRoot(sourceRoot.toPath(), result);
        }
    }
}
