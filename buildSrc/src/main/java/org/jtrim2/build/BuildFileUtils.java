package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.Project;

public final class BuildFileUtils {
    public static Path rootPath(Project project, String... subPaths) {
        return subPath(project.getRootDir(), subPaths);
    }

    public static Path subPath(File base, String... subPaths) {
        return subPath(base.toPath(), subPaths);
    }

    public static Path subPath(Path base, String... subPaths) {
        Path result = base;
        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    public static String readTextFile(Path path) throws IOException {
        return readTextFile(path, StandardCharsets.UTF_8);
    }

    public static String readTextFile(Path path, Charset charset) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, charset);
    }

    private BuildFileUtils() {
        throw new AssertionError();
    }
}
