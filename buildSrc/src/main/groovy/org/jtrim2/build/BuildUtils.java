package org.jtrim2.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
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

    private static <T> Collection<? extends T> emptyIfNull(@Nullable Collection<? extends T> src) {
        return src != null ? src : Collections.emptyList();
    }

    public static <T> List<T> withSafeAppended(
            @Nullable Collection<? extends T> part1,
            @Nullable Collection<? extends T> part2) {

        Collection<? extends T> part1Safe = emptyIfNull(part1);
        Collection<? extends T> part2Safe = emptyIfNull(part2);

        List<T> result = new ArrayList<>(part1Safe.size() + part2Safe.size());
        result.addAll(part1Safe);
        result.addAll(part2Safe);
        return result;
    }

    private BuildUtils() {
        throw new AssertionError();
    }
}
