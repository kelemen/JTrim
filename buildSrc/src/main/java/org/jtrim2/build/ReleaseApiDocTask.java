package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(
        because = "We are always pushing new changes"
)
public class ReleaseApiDocTask extends DefaultTask {
    private static final String API_DIR_NAME = "api";

    private final ObjectFactory objects;

    private final Property<GitRepoService> apiDocsRepoServiceRef;
    private final Property<String> targetBranchName;
    private final DirectoryProperty javadocSourceDir;
    private final Property<String> projectDisplayName;
    private final Property<String> projectVersion;

    @Inject
    public ReleaseApiDocTask(ObjectFactory objects) {
        this.objects = Objects.requireNonNull(objects, "objects");

        this.targetBranchName = objects.property(String.class);
        this.apiDocsRepoServiceRef = objects.property(GitRepoService.class);
        this.javadocSourceDir = objects.directoryProperty();
        this.projectDisplayName = objects.property(String.class);
        this.projectVersion = objects.property(String.class);
    }

    @Internal
    public Property<GitRepoService> getApiDocsRepoServiceRef() {
        return apiDocsRepoServiceRef;
    }

    @Input
    public Property<String> getTargetBranchName() {
        return targetBranchName;
    }

    @InputDirectory
    public DirectoryProperty getJavadocSourceDir() {
        return javadocSourceDir;
    }

    @Input
    public Property<String> getProjectDisplayName() {
        return projectDisplayName;
    }

    @Input
    public Property<String> getProjectVersion() {
        return projectVersion;
    }

    @TaskAction
    public void releaseApiDoc() throws Exception {
        String branchNAme = targetBranchName.get();
        File sourceDir = javadocSourceDir.get().getAsFile();
        String commitMessage = getCommitMessage();

        GitRepoService service = apiDocsRepoServiceRef.get();
        File repoRoot = service.getRepoRoot();

        GitWrapper git = service.git(objects);

        git.clean();

        git.checkoutBranchMaybeRemoteOrDefault(branchNAme, "master");
        prepareContent(sourceDir, new File(repoRoot, API_DIR_NAME));
        git.addAllInDir(repoRoot.toPath(), API_DIR_NAME);
        git.commmitAll(commitMessage);
    }

    private String getCommitMessage() {
        return "Added API doc for " + projectDisplayName.get() + " " + projectVersion.get() + ".";
    }

    private static void prepareContent(File javadocSourceDir, File apiDocPath) throws IOException {
        FileUtils.deleteDirectory(apiDocPath);
        FileUtils.copyDirectory(javadocSourceDir, apiDocPath, false);
    }
}
