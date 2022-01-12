package org.jtrim2.build;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

public final class PackageUtils {
    public static void collectPackageListFromSourceRoot(Path sourceRoot, Set<String> result) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return;
        }

        FileCounter rootCounter = new FileCounter();
        Files.walkFileTree(sourceRoot, new FileVisitor<>() {
            private final Deque<FileCounter> counters = new ArrayDeque<>();
            private FileCounter topCounter = rootCounter;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                counters.push(topCounter);
                topCounter = new FileCounter();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                topCounter.fileCount++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                if (topCounter.fileCount > 0) {
                    result.add(toPackageName(sourceRoot.relativize(dir)));
                }
                topCounter = counters.pop();
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String toPackageName(Path relPath) {
        StringBuilder packageName = new StringBuilder();
        for (Path name: relPath) {
            if (packageName.length() > 0) {
                packageName.append('.');
            }
            packageName.append(name.toString());
        }
        return packageName.toString();
    }

    private static final class FileCounter {
        public int fileCount = 0;
    }

    private PackageUtils() {
        throw new AssertionError();
    }
}
