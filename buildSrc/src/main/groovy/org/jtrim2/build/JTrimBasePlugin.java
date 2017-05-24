package org.jtrim2.build;

import java.io.File;
import java.util.Collections;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public final class JTrimBasePlugin implements Plugin<Project> {
    private static final String FORCED_EVALUATE_TASK_NAME = "evaluate";

    @Override
    public void apply(Project project) {
        try {
            applyUnsafe(project);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void applyUnsafe(Project project) throws Exception {
        ProjectUtils.applyPlugin(project, "base"); // To add "clean" task

        Versions.setVersion(project);
        setupRepositories(project);

        project.getExtensions().add("projectInfo", new JTrimProjectInfo(project));

        project.getTasks().create(FORCED_EVALUATE_TASK_NAME);
    }

    public static void requireEvaluate(Task task, Project requiredProject) {
        if (requiredProject.getParent() == null) {
            return;
        }
        task.dependsOn(requiredProject.getPath() + ":" + FORCED_EVALUATE_TASK_NAME);
    }

    public static void requireEvaluateSubprojects(Task task) {
        Project parent = task.getProject();
        parent.subprojects((subproject) -> {
            requireEvaluate(task, subproject);
        });
    }

    private void setupRepositories(Project project) {
        File repoDefFile = BuildFileUtils.rootPath(project, "gradle", "repositories.gradle").toFile();
        project.apply(Collections.singletonMap("from", repoDefFile));
    }
}
