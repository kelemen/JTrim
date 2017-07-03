package org.jtrim2.build;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.util.PatternSet;

public final class GitWrapper {
    private final Project project;
    private final Git git;

    public GitWrapper(Project project, Repository repository) {
        this.project = Objects.requireNonNull(project, "project");
        this.git = new Git(repository);
    }

    public Git git() {
        return git;
    }

    public CheckoutResult checkoutBranch(String branchName) throws GitAPIException {
        CheckoutCommand checkout = git.checkout();
        checkout.setCreateBranch(false);
        checkout.setName(branchName);
        checkout.call();
        return checkout.getResult();
    }

    public DirCache addAllInDir(Path workingDirRoot, String subDirName) throws GitAPIException {
        AddCommand addCommand = git.add();

        PatternSet pattern = new PatternSet();
        pattern.include(subDirName + "/**");

        FileTree includePath = project.fileTree(workingDirRoot.toFile(), (Action<Object>) null).matching(pattern);
        includePath.visit(new FileVisitor() {
            @Override
            public void visitDir(FileVisitDetails arg0) {
            }

            @Override
            public void visitFile(FileVisitDetails arg0) {
                addCommand.addFilepattern(arg0.getPath());
            }
        });

        addCommand.setUpdate(false);
        return addCommand.call();
    }

    public RevCommit commmitAll(String message) throws GitAPIException {
        CommitCommand commit = git.commit();
        commit.setAll(true);
        commit.setMessage(message);
        return commit.call();
    }

    public Set<String> clean() throws GitAPIException {
        return git.clean().call();
    }
}
