package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

public final class JTrimJavaBasePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        ProjectUtils.applyPlugin(project, JTrimBasePlugin.class);

        ProjectUtils.applyPlugin(project, "java-library");
        configureJava(project);

        setupTravis(project);

        project.afterEvaluate(this::afterEvaluate);
    }

    private void afterEvaluate(Project project) {
        // We are setting this, so the pom.xml will be generated properly
        System.setProperty("line.separator", "\n");
    }

    private void configureJava(Project project) {
        JavaPluginExtension java = ProjectUtils.java(project);

        java.toolchain(spec -> {
            String javaVersion = ProjectUtils.getVersion(project, "java");
            spec.getLanguageVersion().set(JavaLanguageVersion.of(javaVersion));
        });

        TaskContainer tasks = project.getTasks();

        tasks.withType(JavaCompile.class).configureEach(compile -> {
            CompileOptions options = compile.getOptions();
            options.setEncoding("UTF-8");
            options.setCompilerArgs(List.of("-Xlint"));
        });

        JavaPluginExtension javaExt = project.getExtensions().getByType(JavaPluginExtension.class);

        javaExt.withSourcesJar();
        javaExt.withJavadocJar();

        setDefaultDependencies(project);
    }

    private void setDefaultDependencies(Project project) {
        project.getDependencies()
                .add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, ProjectUtils.getBundle(project, "testLibs"));
    }

    private static List<File> emptyForNull(File[] array) {
        return array != null ? List.of(array) : Collections.emptyList();
    }

    private void setupTravis(Project project) {
        if (!project.hasProperty("printTestErrorXmls")) {
            return;
        }

        project.getTasks().named(JavaPlugin.TEST_TASK_NAME, Test.class, test -> {
            test.setIgnoreFailures(true);
            test.doLast(task -> {
                int numberOfFailures = 0;
                File destination = test.getReports().getJunitXml().getOutputLocation().get().getAsFile();
                for (File file: emptyForNull(destination.listFiles())) {
                    String nameLowerCase = file.getName().toLowerCase(Locale.ROOT);
                    if (nameLowerCase.startsWith("test-") && nameLowerCase.endsWith(".xml")) {
                        if (printIfFailing(file)) {
                            numberOfFailures++;
                        }
                    }
                }

                if (numberOfFailures > 0) {
                    throw new RuntimeException("There were ${numberOfFailures} failing test classes.");
                }
            });
        });
    }

    private boolean printIfFailing(File file) {
        try {
            String content = BuildFileUtils.readTextFile(file.toPath(), Charset.defaultCharset());

            if (content.contains("</failure>")) {
                System.out.println("Failing test " + file.getName() + ":\n" + content + "\n\n");
                return true;
            }
        } catch (IOException ex) {
            // ignore silently
        }
        return false;
    }
}
