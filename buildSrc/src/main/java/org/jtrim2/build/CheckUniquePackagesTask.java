package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

@UntrackedTask(
        because = "Too complicated to track."
)
public class CheckUniquePackagesTask extends DefaultTask {
    @Inject
    public CheckUniquePackagesTask() {
    }

    @TaskAction
    public void checkPackages() throws IOException {
        // FIXME: Accessing the project instance will interfere with the configuration cache in the future.
        Project rootProject = getProject();

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

    private static Set<String> findAllPackagesOfProject(Project project) throws IOException {
        Set<String> packages = new HashSet<>();
        collectPackageListFromSources(project, packages);
        return packages;
    }

    private static void collectPackageListFromSources(Project project, Set<String> result) throws IOException {
        JavaPluginExtension java = ProjectUtils.tryGetJava(project);
        if (java == null) {
            return;
        }

        for (SourceSet sourceSet : java.getSourceSets()) {
            for (File sourceRoot: sourceSet.getAllJava().getSrcDirs()) {
                PackageUtils.collectPackageListFromSourceRoot(sourceRoot.toPath(), result);
            }
        }
    }
}
