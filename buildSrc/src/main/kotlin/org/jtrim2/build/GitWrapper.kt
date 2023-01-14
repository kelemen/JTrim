package org.jtrim2.build

import java.nio.file.Path
import org.eclipse.jgit.api.CheckoutResult
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.RemoteConfig
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.util.PatternSet

private const val PREFERRED_REMOTE_NAME = "origin"

class GitWrapper(private val objects: ObjectFactory, repository: Repository?) {
    private val git = Git(repository)

    fun jgit(): Git {
        return git
    }

    fun checkoutBranch(branchName: String): CheckoutResult {
        val checkout = git.checkout()
        checkout.setCreateBranch(false)
        checkout.setName(branchName)
        checkout.call()
        return checkout.result
    }

    val remotes: List<RemoteConfig>
        get() = git.remoteList().call()

    fun tryFindDefaultRemote(): String? {
        var bestMatch: String? = null
        for (remote in remotes) {
            val candidate = remote.name
            if (PREFERRED_REMOTE_NAME == candidate) {
                return PREFERRED_REMOTE_NAME
            }
            if (bestMatch == null) {
                bestMatch = candidate
            }
        }
        return bestMatch
    }

    fun tryFindRemoteForBranch(branchName: String): String? {
        for (remote in remotes) {
            val remoteName = remote.name
            if (hasBranch(toRemoteRef(branchName, remoteName))) {
                return remoteName
            }
        }
        return null
    }

    fun createTrackedBranch(branchName: String, remoteName: String) {
        val branchCreate = git.branchCreate()
        branchCreate.setName(branchName)
        branchCreate.setStartPoint(toRemoteRef(branchName, remoteName))
        branchCreate.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
        branchCreate.call()
    }

    fun checkoutBranchMaybeRemoteOrDefault(
        branchName: String,
        defaultStartingPoint: String
    ) {

        if (hasLocalBranch(branchName)) {
            checkoutBranch(branchName)
            return
        }

        var remoteName = tryFindRemoteForBranch(branchName)
        if (remoteName == null) {
            remoteName = checkNotNull(tryFindDefaultRemote()) { "Couldn't find any remote for new $branchName" }
            createEmptyBranch(branchName, remoteName, defaultStartingPoint)
            return
        }

        createTrackedBranch(branchName, remoteName)
        checkoutBranch(branchName)
    }

    fun createEmptyBranch(branchName: String, remoteName: String, startingPoint: String) {
        val branchCreate = git.branchCreate()
        branchCreate.setName(branchName)
        branchCreate.setStartPoint(startingPoint)
        branchCreate.call()

        val config = git.repository.config
        config.setString(
            ConfigConstants.CONFIG_BRANCH_SECTION,
            branchName,
            ConfigConstants.CONFIG_KEY_REMOTE,
            remoteName
        )
        config.setString(
            ConfigConstants.CONFIG_BRANCH_SECTION,
            branchName,
            ConfigConstants.CONFIG_KEY_MERGE,
            Constants.R_HEADS + branchName
        )

        checkoutBranch(branchName)
    }

    fun addAllInDir(workingDirRoot: Path, subDirName: String): DirCache {
        val addCommand = git.add()

        val pattern = PatternSet()
        pattern.include("$subDirName/**")

        objects.fileTree()
            .from(workingDirRoot.toFile())
            .matching(pattern)
            .visit(object : FileVisitor {
                override fun visitDir(fileRef: FileVisitDetails) {}
                override fun visitFile(fileRef: FileVisitDetails) {
                    addCommand.addFilepattern(fileRef.path)
                }
            })

        addCommand.isUpdate = false
        return addCommand.call()
    }

    fun commmitAll(message: String): RevCommit {
        val commit = git.commit()
        commit.setAll(true)
        commit.message = message
        return commit.call()
    }

    fun clean(): Set<String> {
        return git.clean().call()
    }

    fun hasLocalBranch(branchName: String): Boolean {
        return hasBranch(toQualifiedBranchName(branchName))
    }

    fun hasBranch(qualifiedBranchName: String): Boolean {
        return git.repository.exactRef(qualifiedBranchName) != null
    }
}

private fun toRemoteRef(branchName: String, remoteName: String): String {
    return Constants.R_REMOTES + remoteName + "/" + branchName
}

private fun toQualifiedBranchName(branchName: String): String {
    return Constants.R_HEADS + branchName
}
