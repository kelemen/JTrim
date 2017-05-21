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
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;

public final class JTrimJavaBasePlugin implements Plugin<Project> {
    private static final JavaVersion JAVA_VERSION = JavaVersion.VERSION_1_8;

    private static final String JUNIT_VERSION = "4.11";
    private static final String MOCKITO_VERSION = "1.10.19";

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

        ProjectUtils.applyPlugin(project, "java");
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

        java.setSourceCompatibility(JAVA_VERSION);
        java.setTargetCompatibility(JAVA_VERSION);

        TaskContainer tasks = project.getTasks();

        tasks.withType(JavaCompile.class, (compile) -> {
            CompileOptions options = compile.getOptions();
            options.setEncoding("UTF-8");
            options.setCompilerArgs(Arrays.asList("-Xlint"));
        });

        Jar sourcesJar = tasks.create("sourcesJar", Jar.class, (Jar jar) -> {
            jar.dependsOn("classes");
            jar.setDescription("Creates a jar from the source files.");

            jar.setClassifier("sources");
            jar.from(java.getSourceSets().getByName("main").getAllSource());
        });

        project.artifacts((artifacts) -> {
            artifacts.add("archives", tasks.getByName("jar"));
            artifacts.add("archives", sourcesJar);
        });

        setDefaultDependencies(project);
    }

    private void setDefaultDependencies(Project project) {
        DependencyHandler dependencies = project.getDependencies();

        dependencies.add("testCompile", "junit:junit:" + JUNIT_VERSION);
        dependencies.add("testCompile", "org.mockito:mockito-core:" + MOCKITO_VERSION);
    }

    private void setupTravis(Project project) {
        if (!project.hasProperty("printTestErrorXmls")) {
            return;
        }

        project.getGradle().projectsEvaluated((gradle) -> {
            Test test = (Test)project.getTasks().getByName("test");
            test.setIgnoreFailures(true);
            test.doLast((task) -> {
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
