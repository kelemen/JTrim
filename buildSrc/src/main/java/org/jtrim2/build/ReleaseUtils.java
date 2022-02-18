package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.javadoc.Javadoc;

import static org.jtrim2.build.BuildFileUtils.*;

public final class ReleaseUtils {
    private static final String VERSION_FILE = "version.txt";
    private static final String RELEASE_TASK_NAME = "release";
    private static final String DO_RELEASE_PROPERTY = "doRelease";

    private static final List<String> RELEASE_TASKS = Collections.unmodifiableList(Arrays.asList(
            "releaseApiDoc",
            "publish"
    ));

    public static boolean isRelease(Project project) {
        return project.hasProperty(DO_RELEASE_PROPERTY);
    }

    public static void setupMainReleaseTask(Project project) {
        setupReleaseTasks(project).configure(releaseProject -> {
            releaseProject.setDescription("Releases JTrim if the doRelease property is defined.");

            if (!isRelease(project)) {
                releaseProject.doFirst(BuildUtils.lambdaAction(task -> {
                    throw new RuntimeException("You must specify the '-P" + DO_RELEASE_PROPERTY
                            + "' argument to execute the release task.");
                }));
            }

            releaseProject.doLast(BuildUtils.lambdaAction(task -> {
                try {
                    releaseMain(project);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }));
        });
    }

    private static void releaseMain(Project project) throws Exception {
        try (Repository gitRepo = FileRepositoryBuilder.create(rootPath(project, ".git").toFile())) {
            Git git = new Git(gitRepo);

            StatusCommand statusCommand = git.status();
            Status status = statusCommand.call();

            if (!status.getUntracked().isEmpty()) {
                throw new RuntimeException("There are untracked files in the repository and so the release cannot be completed. Revert the changes already done manually.");
            }
            if (!status.isClean()) {
                throw new RuntimeException("The repository is not clean (contains uncommited changes) and so the release cannot be completed. Revert the changes already done manually.");
            }

            TagCommand tagCommand = git.tag();
            tagCommand.setName("v" + project.getVersion());
            tagCommand.setMessage("Release of JTrim " + project.getVersion());
            tagCommand.call();

            String nextVersion = setNextVersion(project);

            AddCommand addCommand = git.add();
            addCommand.addFilepattern(VERSION_FILE);
            addCommand.setUpdate(true);
            addCommand.call();

            CommitCommand commitCommand = git.commit();
            commitCommand.setMessage("Set the version to " + nextVersion);
            commitCommand.call();
        }

        System.out.println("New Release: " + project.getGroup() + ":" + project.getName() + ":" + project.getVersion());
    }

    public static TaskProvider<Task> setupReleaseTasks(Project project) {
        setupPublishDocs(project);

        TaskContainer tasks = project.getTasks();

        TaskProvider<Task> releaseProjectRef = tasks.register(RELEASE_TASK_NAME, releaseProject -> {
            Stream<String> existintTasks = RELEASE_TASKS.stream()
                    .filter(taskName -> BuildUtils.tryGetTaskRef(tasks, taskName, Task.class) != null);
            releaseProject.dependsOn(existintTasks.toArray());
        });

        Project parent = project.getParent();
        if (parent != null) {
            parent.getTasks().named(RELEASE_TASK_NAME, releaseTaskRef -> {
                releaseTaskRef.dependsOn(releaseProjectRef);
            });
        }

        return releaseProjectRef;
    }

    private static void setupPublishDocs(Project project) {
        project.getTasks().register("releaseApiDoc", releaseApiDoc -> {
            releaseApiDoc.dependsOn(JavaPlugin.JAVADOC_TASK_NAME);
            releaseApiDoc.doLast(BuildUtils.lambdaAction(task -> {
                try {
                    releaseApiDoc(project);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }));
        });
    }

    private static void releaseApiDoc(Project project) throws Exception {
        String releaseApiDocRepo = ProjectUtils.getStringProperty(project, "releaseApiDocRepo", null);
        if (releaseApiDocRepo == null) {
            throw new RuntimeException("You must specify the the 'releaseApiDocRepo' property to publish the javadoc.");
        }

        Path apiDocRoot = Paths.get(releaseApiDocRepo);
        if (!Files.isDirectory(apiDocRoot)) {
            throw new RuntimeException("The directory " + apiDocRoot + " does not exist.");
        }

        if (!Files.isDirectory(apiDocRoot.resolve(".git"))) {
            throw new RuntimeException("'The directory " + apiDocRoot + " is not a git repository.");
        }

        releaseApiDoc(project, apiDocRoot);
    }

    private static void releaseApiDoc(Project project, Path apiDocRoot) throws Exception {
        String apiDirName = "api";
        Path apiDocPath = apiDocRoot.resolve(apiDirName);
        String branchName = project.getName();

        Provider<File> javadocOutputDir = javadocOutputDir(project);

        try (Repository gitRepo = FileRepositoryBuilder.create(apiDocRoot.resolve(".git").toFile())) {
            GitWrapper git = new GitWrapper(project.getObjects(), gitRepo);

            git.clean();

            git.checkoutBranchMaybeRemoteOrDefault(branchName, "master");
            prepareContent(javadocOutputDir, apiDocPath.toFile());
            git.addAllInDir(apiDocRoot, apiDirName);
            git.commmitAll(getApiDocMessage(project));
        }
    }

    private static String getApiDocMessage(Project project) {
        JTrimProjectInfo projectInfo = ProjectUtils.getProjectInfo(project);
        return "Added API doc for " + projectInfo.getDisplayName().get() + " " + project.getVersion() + ".";
    }

    private static Provider<File> javadocOutputDir(Project project) {
        return project
                .getTasks()
                .named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc.class)
                .map(javadoc -> Objects.requireNonNull(javadoc.getDestinationDir()));
    }

    private static void prepareContent(Provider<File> javadocOutputDir, File apiDocPath) throws IOException {
        FileUtils.deleteDirectory(apiDocPath);
        FileUtils.copyDirectory(javadocOutputDir.get(), apiDocPath, false);
    }

    private static String incVersion(String version) {
        int sepIndex = version.lastIndexOf('.');
        String prefix = version.substring(0, sepIndex);
        int patchVersion = Integer.parseInt(version.substring(sepIndex + 1, version.length()));
        return prefix + '.' + (patchVersion + 1);
    }

    private static String setNextVersion(Project project) throws IOException {
        String nextVersion = incVersion(Versions.getVersionBase(project));

        Path versionFile = rootPath(project, VERSION_FILE);
        Files.writeString(versionFile, nextVersion);
        return nextVersion;
    }

    private ReleaseUtils() {
        throw new AssertionError();
    }
}
