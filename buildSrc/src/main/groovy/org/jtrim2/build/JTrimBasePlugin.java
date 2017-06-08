package org.jtrim2.build;

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

        ProjectUtils.applyScript(project, "repositories.gradle");
        ProjectUtils.applyScript(project, "dependencies.gradle");

        project.getExtensions().add("projectInfo", new JTrimProjectInfo(project));

        project.getTasks().create(FORCED_EVALUATE_TASK_NAME);
    }

    public static void requireTaskOfProject(Task task, Project requiredProject, String dependency) {
        if (requiredProject.getParent() == null) {
            return;
        }
        task.dependsOn(requiredProject.getPath() + ":" + dependency);
    }

    public static void requireEvaluate(Task task, Project requiredProject) {
        requireTaskOfProject(task, requiredProject, FORCED_EVALUATE_TASK_NAME);
    }

    public static void requireEvaluateSubprojects(Task task) {
        requireEvaluateSubprojects(task, task.getProject());
    }

    public static void requireEvaluateSubprojects(Task task, Project parent) {
        requireSubprojectsTask(task, parent, FORCED_EVALUATE_TASK_NAME);
    }

    public static void requireSubprojectsTask(Task task, String dependency) {
        requireSubprojectsTask(task, task.getProject(), dependency);
    }

    public static void requireSubprojectsTask(Task task, Project parent, String dependency) {
        parent.subprojects((subproject) -> {
            requireTaskOfProject(task, subproject, dependency);
        });
    }
}
