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

        project.getExtensions().add("projectInfo", new JTrimProjectInfo(project));

        project.getTasks().register(FORCED_EVALUATE_TASK_NAME);
    }

    private static String qualifiedDependency(Project project, String relativeTaskName) {
        return project.getPath() + ":" + relativeTaskName;
    }

    public static void requireTaskOfProject(Task task, Project requiredProject, String relativeTaskName) {
        if (requiredProject.getParent() == null) {
            return;
        }

        task.dependsOn(qualifiedDependency(requiredProject, relativeTaskName));
    }

    public static void requireEvaluateSubprojects(Task task) {
        requireEvaluateSubprojects(task, task.getProject());
    }

    public static void requireEvaluateSubprojects(Task task, Project parent) {
        requireSubprojectsTask(task, parent, FORCED_EVALUATE_TASK_NAME);
    }

    public static void requireSubprojectsTask(Task task, Project parent, String dependency) {
        parent.subprojects((subproject) -> {
            requireTaskOfProject(task, subproject, dependency);
        });
    }
}
