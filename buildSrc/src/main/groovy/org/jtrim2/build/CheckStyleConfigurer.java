package org.jtrim2.build;

import java.util.Objects;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;

import static org.jtrim2.build.BuildFileUtils.*;

public final class CheckStyleConfigurer {
    private static final String VERSION = "7.6.1";

    private static final String CONFIG_FILE_MAIN = "jtrim-style.xml";
    private static final String CONFIG_FILE_TEST = "jtrim-test-style.xml";

    private final Project project;

    public CheckStyleConfigurer(Project project) {
        this.project = Objects.requireNonNull(project);
    }

    public void configure() {
        ProjectUtils.applyPlugin(project, "checkstyle");

        project.getExtensions().configure(CheckstyleExtension.class, (CheckstyleExtension checkstyle) -> {
            checkstyle.setConfigFile(rootPath(project, CONFIG_FILE_MAIN).toFile());
            checkstyle.setToolVersion(VERSION);
        });

        project.getTasks().getByName("checkstyleMain", (task) -> {
            Checkstyle checkstyleMain = (Checkstyle)task;
            checkstyleMain.setClasspath(lazyClasspath("main", "runtime"));
        });

        project.getTasks().getByName("checkstyleTest", (task) -> {
            Checkstyle checkstyleTest = (Checkstyle)task;
            checkstyleTest.setClasspath(lazyClasspath("test", "testRuntime"));
            checkstyleTest.setConfigFile(rootPath(project, CONFIG_FILE_TEST).toFile());
        });
    }

    private FileCollection lazyClasspath(String sourceSetName, String configName) {
        return project.files(GroovyUtils.toSupplierClosure(() -> {
            JavaPluginConvention java = ProjectUtils.java(project);

            SourceSet sourceSet = java.getSourceSets().getByName(sourceSetName);
            SourceSetOutput sourceSetOutput = sourceSet.getOutput();

            Configuration config = project.getConfigurations().getByName(configName);

            return project.files(sourceSetOutput, config.resolve());
        }));
    }
}
