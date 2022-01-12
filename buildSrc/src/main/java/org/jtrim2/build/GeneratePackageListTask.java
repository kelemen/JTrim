package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

@UntrackedTask(
        because = "Too complicated to track."
)
public class GeneratePackageListTask extends DefaultTask {
    public static final String DEFAULT_TASK_NAME = "mainPackageList";

    private final Provider<Set<String>> packageList;
    private final RegularFileProperty packageListFile;

    @Inject
    public GeneratePackageListTask(ProjectLayout layout, ObjectFactory objects, ProviderFactory providers) {
        this.packageListFile = objects.fileProperty();
        this.packageListFile.set(layout.file(layout.getBuildDirectory().map(p -> {
            return p.getAsFile().toPath().resolve(getName()).resolve("package-list").toFile();
        })));
        this.packageList = providers.provider(() -> {
            Set<String> packages = new HashSet<>();
            try {
                collectPackageListFromSources(getProject(), packages);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return packages;
        });
    }

    @OutputFile
    public RegularFileProperty getPackageListFile() {
        return packageListFile;
    }

    @TaskAction
    public void generatePackageList() throws IOException {
        List<String> sortedPackages = new ArrayList<>(packageList.get());
        sortedPackages.sort(String::compareTo);
        sortedPackages.add("");

        Files.writeString(packageListFile.get().getAsFile().toPath(), String.join("\n", sortedPackages));
    }

    private static void collectPackageListFromSources(Project project, Set<String> result) throws IOException {
        if (!ProjectUtils.isReleasedProject(project)) {
            return;
        }

        JavaPluginExtension java = ProjectUtils.tryGetJava(project);
        if (java == null) {
            return;
        }

        SourceSet sourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        for (File sourceRoot: sourceSet.getAllJava().getSrcDirs()) {
            PackageUtils.collectPackageListFromSourceRoot(sourceRoot.toPath(), result);
        }
    }
}
