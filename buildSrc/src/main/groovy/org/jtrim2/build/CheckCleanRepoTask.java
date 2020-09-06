package org.jtrim2.build;

import javax.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.storage.file.FileRepository;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import static org.jtrim2.build.BuildFileUtils.*;

public class CheckCleanRepoTask extends DefaultTask {
    @Inject
    public CheckCleanRepoTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    @TaskAction
    public void checkCleanRepo() throws Exception {
        FileRepository gitRepo = new FileRepository(rootPath(getProject(), ".git").toFile());
        try {
            Git git = new Git(gitRepo);

            checkCleanRepo(git);
        } finally {
            gitRepo.close();
        }
    }

    public static void checkCleanRepo(Git git) throws Exception {
        StatusCommand statusCommand = git.status();
        Status status = statusCommand.call();

        if (!status.getUntracked().isEmpty()) {
            throw new RuntimeException("There are untracked files in the repository and so the release cannot be completed. Revert the changes already done manually.");
        }
        if (!status.isClean()) {
            throw new RuntimeException("The repository is not clean (contains uncommited changes) and so the release cannot be completed. Revert the changes already done manually.");
        }
    }
}
