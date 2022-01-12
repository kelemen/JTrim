package org.jtrim2.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

public final class BuildUtils {
    public static <T extends Task> TaskProvider<T> tryGetTaskRef(TaskContainer tasks, String name, Class<T> taskType) {
        try {
            return tasks.named(name, taskType);
        } catch (UnknownTaskException ex) {
            return null;
        }
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

    private BuildUtils() {
        throw new AssertionError();
    }
}
