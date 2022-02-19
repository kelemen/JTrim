package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.inject.Inject;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.TagCommand;
import org.gradle.api.DefaultTask;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(
        because = "We are always pushing new changes"
)
public class FinalReleaseTask extends DefaultTask {
    private static final String VERSION_FILE = "version.txt";

    private final ObjectFactory objects;
    private final Property<GitRepoService> repoServiceRef;
    private final Property<String> projectBaseVersion;
    private final Property<String> projectVersion;

    @Inject
    public FinalReleaseTask(ObjectFactory objects) {
        this.objects = Objects.requireNonNull(objects, "objects");
        this.repoServiceRef = objects.property(GitRepoService.class);
        this.projectBaseVersion = objects.property(String.class);
        this.projectVersion = objects.property(String.class);
    }

    @Internal
    public Property<GitRepoService> getRepoServiceRef() {
        return repoServiceRef;
    }

    @Input
    public Property<String> getProjectBaseVersion() {
        return projectBaseVersion;
    }

    @Input
    public Property<String> getProjectVersion() {
        return projectVersion;
    }

    @TaskAction
    public void finalizeRelease() throws Exception {
        String projectVersion = this.projectVersion.get();
        String projectBaseVersion = this.projectBaseVersion.get();


        GitRepoService repoService = repoServiceRef.get();
        Git git = repoService.git(objects).jgit();

        StatusCommand statusCommand = git.status();
        Status status = statusCommand.call();

        if (!status.getUntracked().isEmpty()) {
            throw new RuntimeException("There are untracked files in the repository and so the release cannot be completed. Revert the changes already done manually.");
        }
        if (!status.isClean()) {
            throw new RuntimeException("The repository is not clean (contains uncommitted changes) and so the release cannot be completed. Revert the changes already done manually.");
        }

        TagCommand tagCommand = git.tag();
        tagCommand.setName("v" + projectVersion);
        tagCommand.setMessage("Release of JTrim " + projectVersion);
        tagCommand.call();

        String nextVersion = setNextVersion(repoService.getRepoRoot(), projectBaseVersion);

        AddCommand addCommand = git.add();
        addCommand.addFilepattern(VERSION_FILE);
        addCommand.setUpdate(true);
        addCommand.call();

        CommitCommand commitCommand = git.commit();
        commitCommand.setMessage("Set the version to " + nextVersion);
        commitCommand.call();

        System.out.println("New Release: " + projectVersion + ", Next version = " + nextVersion);
    }

    private String setNextVersion(File repoRoot, String baseVersion) throws IOException {
        String nextVersion = incVersion(baseVersion);

        Path versionFile = repoRoot.toPath().resolve(VERSION_FILE);
        Files.writeString(versionFile, nextVersion);
        return nextVersion;
    }

    private static String incVersion(String version) {
        int sepIndex = version.lastIndexOf('.');
        String prefix = version.substring(0, sepIndex);
        int patchVersion = Integer.parseInt(version.substring(sepIndex + 1));
        return prefix + '.' + (patchVersion + 1);
    }
}
