package org.jtrim2.build;

import org.gradle.api.Action;
import org.gradle.api.Task;

public final class BuildUtils {
    public static Task lazilyConfiguredTask(Task task, Action<? super Task> taskConfiguration) {
        String configTaskName = "configure" + capitalizeFirst(task.getName());
        Task configTask = task.getProject().getTasks().findByName(configTaskName);
        if (configTask == null) {
            configTask = task.getProject().task(configTaskName);
            task.dependsOn(configTask);
        }

        configTask.doLast((Task ignore) -> {
            taskConfiguration.execute(task);
        });

        return task;
    }

    public static String capitalizeFirst(String str) {
        if (str.isEmpty()) {
            return str;
        }

        char firstCh = str.charAt(0);
        char newFirstCh = Character.toUpperCase(firstCh);
        if (firstCh == newFirstCh) {
            return str;
        }

        return newFirstCh + str.substring(1);
    }

    private BuildUtils() {
        throw new AssertionError();
    }
}
