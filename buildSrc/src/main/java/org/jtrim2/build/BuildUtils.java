package org.jtrim2.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

public final class BuildUtils {
    public static Task lazilyConfiguredTask(Task task, Action<? super Task> taskConfiguration) {
        TaskProvider<Task> configTaskRef = getConfigTaskForTask(task.getProject(), task.getName(), task::dependsOn);
        configTaskRef.configure(configTask -> {
            configTask.doLast(ignore -> {
                taskConfiguration.execute(task);
            });
        });

        return task;
    }

    private static TaskProvider<Task> getConfigTaskForTask(
            Project project,
            String taskName,
            Consumer<TaskProvider<Task>> configTaskInitializer) {

        String configTaskName = "configure" + capitalizeFirst(taskName);

        TaskContainer tasks = project.getTasks();
        TaskProvider<Task> result = tryGetTaskRef(tasks, configTaskName, Task.class);
        if (result == null) {
            result = tasks.register(configTaskName);
            configTaskInitializer.accept(result);
        }
        return result;
    }

    public static <T extends Task> TaskProvider<T> tryGetTaskRef(TaskContainer tasks, String name, Class<T> taskType) {
        try {
            return tasks.named(name, taskType);
        } catch (UnknownTaskException ex) {
            return null;
        }
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

    public static <T, R> List<R> flatMapToReadOnly(
            Collection<? extends T> src,
            Function<? super T, ? extends Stream<? extends R>> mapper) {

        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(mapper, "mapper");

        List<R> result = new ArrayList<>();
        src.forEach(e -> {
            mapper.apply(e).forEach(result::add);
        });
        return Collections.unmodifiableList(result);
    }

    public static <T, R> List<R> mapToReadOnly(Collection<? extends T> src, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(mapper, "mapper");

        List<R> result = new ArrayList<>(src.size());
        src.forEach(e -> result.add(mapper.apply(e)));
        return Collections.unmodifiableList(result);
    }

    public static <T> Collection<? extends T> emptyIfNull(@Nullable Collection<? extends T> src) {
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
