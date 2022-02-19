package org.jtrim2.build;

import java.util.Objects;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.javadoc.Javadoc;

public final class ReleaseUtils {
    private static final String RELEASE_TASK_NAME = "release";
    private static final String DO_RELEASE_PROPERTY = "doRelease";

    public static boolean isRelease(Project project) {
        return project.hasProperty(DO_RELEASE_PROPERTY);
    }

    public static void setupMainReleaseTask(Project project) {
        ReleaseUtils.setupPublishDocs(project);

        project.getTasks().register(RELEASE_TASK_NAME, FinalReleaseTask.class, task -> {
            task.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
            task.setDescription("Releases JTrim if the " + DO_RELEASE_PROPERTY + " property is defined.");
            if (!isRelease(project)) {
                task.doFirst(BuildUtils.lambdaAction(t -> {
                    throw new RuntimeException("You must specify the '-P" + DO_RELEASE_PROPERTY
                            + "' argument to execute the release task.");
                }));
            }

            Provider<GitRepoService> repoServiceRef = GitRepoService
                    .getService(project, GitRepoService.PROJECT_SERVICE_NAME);
            task.getRepoServiceRef().set(repoServiceRef);
            task.usesService(repoServiceRef);

            task.getProjectVersion().set(getVersionRef(project));
            task.getProjectBaseVersion().set(project.getProviders().provider(() -> Versions.getVersionBase(project)));
        });
    }

    public static void setupPublishDocs(Project project) {
        project.getTasks().register("releaseApiDoc", ReleaseApiDocTask.class, task -> {
            task.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
            task.setDescription("Copies and commites the Javadoc files into the given git repository.");

            TaskProvider<Javadoc> javadocRef = project
                    .getTasks()
                    .named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc.class);

            task.dependsOn(javadocRef);

            Provider<GitRepoService> repoServiceRef = GitRepoService
                    .getService(project, GitRepoService.API_DOCS_SERVICE_NAME);
            task.getApiDocsRepoServiceRef().set(repoServiceRef);
            task.usesService(repoServiceRef);

            task.getTargetBranchName().set(project.getName());

            Provider<Directory> javadocsSourceDir = project.getLayout()
                    .dir(javadocRef.map(javadoc -> Objects.requireNonNull(javadoc.getDestinationDir())));
            task.getJavadocSourceDir().set(javadocsSourceDir);

            task.getProjectDisplayName().set(ProjectUtils.getProjectInfo(project).getDisplayName());
            task.getProjectVersion().set(getVersionRef(project));
        });
    }

    private static Provider<String> getVersionRef(Project project) {
        return project.getProviders().provider(() -> project.getVersion().toString());
    }

    private ReleaseUtils() {
        throw new AssertionError();
    }
}
