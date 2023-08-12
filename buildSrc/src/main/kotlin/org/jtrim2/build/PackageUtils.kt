package org.jtrim2.build

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.ArrayDeque
import java.util.Deque

object PackageUtils {
    private val excludedFileNames = setOf("module-info.java")

    fun collectPackageListFromSourceRoot(sourceRoot: Path, result: MutableSet<String>) {
        if (!Files.isDirectory(sourceRoot)) {
            return
        }

        val rootCounter = FileCounter()
        Files.walkFileTree(sourceRoot, object : FileVisitor<Path> {
            private val counters: Deque<FileCounter> = ArrayDeque()
            private var topCounter = rootCounter

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                counters.push(topCounter)
                topCounter = FileCounter()
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!excludedFileNames.contains(file.fileName.toString())) {
                    topCounter.fileCount++
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (topCounter.fileCount > 0) {
                    result.add(toPackageName(sourceRoot.relativize(dir)))
                }
                topCounter = counters.pop()
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun toPackageName(relPath: Path): String {
        val packageName = StringBuilder()
        for (name in relPath) {
            if (packageName.isNotEmpty()) {
                packageName.append('.')
            }
            packageName.append(name.toString())
        }
        return packageName.toString()
    }
}

private class FileCounter {
    var fileCount = 0
}
