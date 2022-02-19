package org.jtrim2.build;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteConfig;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.util.PatternSet;

public final class GitWrapper {
    private static final String PREFERRED_REMOTE_NAME = "origin";

    private final ObjectFactory objects;
    private final Git git;

    public GitWrapper(ObjectFactory objects, Repository repository) {
        this.objects = Objects.requireNonNull(objects, "objects");
        this.git = new Git(repository);
    }

    public Git jgit() {
        return git;
    }

    public CheckoutResult checkoutBranch(String branchName) throws GitAPIException {
        CheckoutCommand checkout = git.checkout();
        checkout.setCreateBranch(false);
        checkout.setName(branchName);
        checkout.call();
        return checkout.getResult();
    }

    public List<RemoteConfig> getRemotes() throws GitAPIException {
        return git.remoteList().call();
    }

    private static String toRemoteRef(String branchName, String remoteName) {
        return Constants.R_REMOTES + remoteName + "/" + branchName;
    }

    public String tryFindDefaultRemote() throws GitAPIException {
        String bestMatch = null;
        for (RemoteConfig remote: getRemotes()) {
            String candidate = remote.getName();
            if (PREFERRED_REMOTE_NAME.equals(candidate)) {
                return PREFERRED_REMOTE_NAME;
            }
            if (bestMatch == null) {
                bestMatch = candidate;
            }
        }
        return bestMatch;
    }

    public String tryFindRemoteForBranch(String branchName) throws GitAPIException {
        for (RemoteConfig remote: getRemotes()) {
            String remoteName = remote.getName();
            if (hasBranch(toRemoteRef(branchName, remoteName))) {
                return remoteName;
            }
        }
        return null;
    }

    public void createTrackedBranch(String branchName, String remoteName) throws GitAPIException {
        CreateBranchCommand branchCreate = git.branchCreate();
        branchCreate.setName(branchName);
        branchCreate.setStartPoint(toRemoteRef(branchName, remoteName));
        branchCreate.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM);
        branchCreate.call();
    }

    public void checkoutBranchMaybeRemoteOrDefault(
            String branchName,
            String defaultStartingPoint) throws GitAPIException {

        if (hasLocalBranch(branchName)) {
            checkoutBranch(branchName);
            return;
        }

        String remoteName = tryFindRemoteForBranch(branchName);
        if (remoteName == null) {
            remoteName = tryFindDefaultRemote();
            if (remoteName == null) {
                throw new IllegalStateException("Couldn't find any remote for new " + branchName);
            }

            createEmptyBranch(branchName, remoteName, defaultStartingPoint);
            return;
        }

        createTrackedBranch(branchName, remoteName);
        checkoutBranch(branchName);
    }

    public void createEmptyBranch(String branchName, String remoteName, String startingPoint) throws GitAPIException {
        CreateBranchCommand branchCreate = git.branchCreate();
        branchCreate.setName(branchName);
        branchCreate.setStartPoint("master");
        branchCreate.call();

        StoredConfig config = git.getRepository().getConfig();
        config.setString(
                ConfigConstants.CONFIG_BRANCH_SECTION,
                branchName,
                ConfigConstants.CONFIG_KEY_REMOTE,
                remoteName
        );
        config.setString(
                ConfigConstants.CONFIG_BRANCH_SECTION,
                branchName,
                ConfigConstants.CONFIG_KEY_MERGE,
                Constants.R_HEADS + branchName
        );

        checkoutBranch(branchName);
    }

    public DirCache addAllInDir(Path workingDirRoot, String subDirName) throws GitAPIException {
        AddCommand addCommand = git.add();

        PatternSet pattern = new PatternSet();
        pattern.include(subDirName + "/**");

        FileTree includePath = objects
                .fileTree()
                .from(workingDirRoot.toFile())
                .matching(pattern);
        includePath.visit(new FileVisitor() {
            @Override
            public void visitDir(FileVisitDetails fileRef) {
            }

            @Override
            public void visitFile(FileVisitDetails fileRef) {
                addCommand.addFilepattern(fileRef.getPath());
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

    private static String toQualifiedBranchName(String branchName) {
        return Constants.R_HEADS + branchName;
    }

    public boolean hasLocalBranch(String branchName) {
        return hasBranch(toQualifiedBranchName(branchName));
    }

    public boolean hasBranch(String qualifiedBranchName) {
        try {
            return git.getRepository().exactRef(qualifiedBranchName) != null;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
