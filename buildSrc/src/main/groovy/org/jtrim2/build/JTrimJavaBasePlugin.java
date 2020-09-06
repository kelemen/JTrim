package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public final class JTrimJavaBasePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        try {
            applyUnsafe(project);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void applyUnsafe(Project project) throws Exception {
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
        JavaPluginConvention java = ProjectUtils.java(project);

        JavaVersion javaVersion = JavaVersion.toVersion(ProjectUtils.getDependencyFor(project, "java"));
        java.setSourceCompatibility(javaVersion);
        java.setTargetCompatibility(javaVersion);

        TaskContainer tasks = project.getTasks();

        tasks.withType(JavaCompile.class).configureEach(compile -> {
            CompileOptions options = compile.getOptions();
            options.setEncoding("UTF-8");
            options.setCompilerArgs(Arrays.asList("-Xlint"));
        });

        TaskProvider<Jar> sourcesJarRef = tasks.register("sourcesJar", Jar.class, jar -> {
            jar.dependsOn("classes");

            jar.setGroup(LifecycleBasePlugin.BUILD_GROUP);
            jar.setDescription("Creates a jar from the source files.");

            jar.getArchiveClassifier().set("sources");
            jar.from(java.getSourceSets().getByName("main").getAllSource());
        });

        TaskProvider<Jar> javadocJarRef = tasks.register("javadocJar", Jar.class, jar -> {
            jar.dependsOn(JavaPlugin.JAVADOC_TASK_NAME);
            jar.setDescription("Creates a jar from the JavaDoc.");

            jar.getArchiveClassifier().set("javadoc");

            jar.from(tasks
                    .named(JavaPlugin.JAVADOC_TASK_NAME, Javadoc.class)
                    .map(Javadoc::getDestinationDir)
            );
        });

        project.artifacts(artifacts -> {
            artifacts.add("archives", tasks.named(JavaPlugin.JAR_TASK_NAME));
            artifacts.add("archives", sourcesJarRef);
            artifacts.add("archives", javadocJarRef);
        });

        setDefaultDependencies(project);
    }

    private void setDefaultDependencies(Project project) {
        DependencyHandler dependencies = project.getDependencies();

        Arrays.asList("junit", "mockito").forEach(dependency -> {
            dependencies.add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, ProjectUtils.getDependencyFor(project, dependency));
        });
    }

    private void setupTravis(Project project) {
        if (!project.hasProperty("printTestErrorXmls")) {
            return;
        }

        project.getTasks().named(JavaPlugin.TEST_TASK_NAME, Test.class, test -> {
            test.setIgnoreFailures(true);
            test.doLast(task -> {
                int numberOfFailures = 0;
                File destination = test.getReports().getJunitXml().getDestination();
                for (File file: destination.listFiles()) {
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
