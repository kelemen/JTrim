package org.jtrim2.taskgraph.basic;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jtrim2.taskgraph.TaskFactoryConfig;
import org.jtrim2.taskgraph.TaskFactoryDefiner;
import org.jtrim2.taskgraph.TaskFactoryGroupConfigurer;
import org.jtrim2.taskgraph.TaskFactoryKey;
import org.jtrim2.taskgraph.TaskFactorySetup;
import org.jtrim2.taskgraph.TaskGraphBuilder;
import org.jtrim2.taskgraph.TaskGraphDefConfigurer;

/**
 * Defines a simple implementation of {@code TaskGraphDefConfigurer} which collects the
 * added task factory definitions and simply passes them to a given {@link TaskGraphBuilderFactory}.
 *
 * <h2>Thread safety</h2>
 * The methods of this class may not be used by multiple threads concurrently, unless otherwise noted.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class are not <I>synchronization transparent</I> in general.
 *
 * @see CollectingTaskGraphBuilder
 * @see RestrictableTaskGraphExecutor
 */
public final class CollectingTaskGraphDefConfigurer implements TaskGraphDefConfigurer {
    private final TaskGraphBuilderFactory graphBuilderFactory;

    private final Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> factoryDefs;

    /**
     * Creates a new {@code CollectingTaskGraphDefConfigurer} with the given
     * {@code TaskGraphBuilderFactory} to be called when the {@link #build() build()}
     * method is called.
     *
     * @param graphBuilderFactory the {@code TaskGraphBuilderFactory} to be called when
     *   the {@code build()} method is called. This argument cannot be {@code null}.
     */
    public CollectingTaskGraphDefConfigurer(TaskGraphBuilderFactory graphBuilderFactory) {
        Objects.requireNonNull(graphBuilderFactory, "graphBuilderFactory");
        this.graphBuilderFactory = graphBuilderFactory;
        this.factoryDefs = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc }
     * <P>
     * This implementation allows this method to be called concurrently and also
     * allows concurrent use of the returned {@code TaskFactoryDefiner}. However,
     * none of them might be used concurrently with the {@link #build() build()}
     * method call.
     */
    @Override
    public TaskFactoryDefiner factoryGroupDefiner(TaskFactoryGroupConfigurer groupConfigurer) {
        return new GroupDefiner(factoryDefs, groupConfigurer);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskGraphBuilder build() {
        return graphBuilderFactory.createGraphBuilder(
                Collections.unmodifiableCollection(factoryDefs.values()));
    }

    private static final class GroupDefiner implements TaskFactoryDefiner {
        private final Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> factoryDefs;
        private final TaskFactoryGroupConfigurer groupConfigurer;

        public GroupDefiner(
                Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> factoryDefs,
                TaskFactoryGroupConfigurer groupConfigurer) {
            this.factoryDefs = factoryDefs;
            this.groupConfigurer = groupConfigurer;
        }

        @Override
        public <R, I> TaskFactoryConfig<R, I> defineFactory(
                TaskFactoryKey<R, I> defKey,
                TaskFactorySetup<R, I> setup) {

            TaskFactoryConfig<R, I> config = new TaskFactoryConfig<>(defKey, groupConfigurer, setup);

            @SuppressWarnings("unchecked")
            TaskFactoryConfig<R, I> prev = (TaskFactoryConfig<R, I>) factoryDefs.put(defKey, config);
            return prev;
        }
    }
}
