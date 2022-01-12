package org.jtrim2.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.tasks.SourceSet;

import static org.jtrim2.build.BuildFileUtils.*;

public final class CheckStyleConfigurer {
    private final Project project;
    private final String type;

    public CheckStyleConfigurer(Project project, String type) {
        this.project = Objects.requireNonNull(project);
        this.type = Objects.requireNonNull(type);
    }

    public void configure() {
        ProjectUtils.applyPlugin(project, "checkstyle");

        project.getExtensions().configure(CheckstyleExtension.class, checkstyle -> {
            checkstyle.setConfigFile(checkStyeConfig(null).toFile());
            checkstyle.setToolVersion(ProjectUtils.getVersionStrFor(project, "checkstyle"));
        });

        project.getTasks().withType(Checkstyle.class).configureEach(task -> {
            String sourceSetName = getSourceSetName(task);
            Path configCandidate = checkStyeConfig(sourceSetName);
            if (Files.isRegularFile(configCandidate)) {
                task.setConfigFile(configCandidate.toFile());
            }

            JavaPluginExtension java = ProjectUtils.java(project);
            SourceSet sourceSet = java.getSourceSets().findByName(sourceSetName);
            if (sourceSet != null) {
                task.setClasspath(sourceSet.getRuntimeClasspath());
            }
        });
    }

    private String getSourceSetName(Checkstyle task) {
        String expectedPrefix = "checkstyle";
        String name = task.getName();

        if (!name.startsWith(expectedPrefix)) {
            return name;
        }

        String sourceSetName = name.substring(expectedPrefix.length());
        return lowerCaseFirst(sourceSetName);
    }

    private static String lowerCaseFirst(String str) {
        if (str.isEmpty()) {
            return str;
        }

        char firstCh = str.charAt(0);
        char lowFirstCh = Character.toLowerCase(firstCh);
        if (firstCh == lowFirstCh) {
            return str;
        }

        return lowFirstCh + str.substring(1);
    }

    private Path checkStyeConfig(String sourceSetName) {
        StringBuilder fileName = new StringBuilder();
        fileName.append("check-style");
        if (sourceSetName != null) {
            fileName.append('-');
            fileName.append(sourceSetName);
        }
        if (!type.isEmpty()) {
            fileName.append('.');
            fileName.append(type);
        }
        fileName.append(".xml");

        return rootPath(project, "gradle", fileName.toString());
    }
}
