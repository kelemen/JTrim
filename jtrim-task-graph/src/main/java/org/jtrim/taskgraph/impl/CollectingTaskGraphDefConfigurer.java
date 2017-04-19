package org.jtrim.taskgraph.impl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jtrim.taskgraph.TaskFactoryConfig;
import org.jtrim.taskgraph.TaskFactoryDefiner;
import org.jtrim.taskgraph.TaskFactoryGroupConfigurer;
import org.jtrim.taskgraph.TaskFactoryKey;
import org.jtrim.taskgraph.TaskFactorySetup;
import org.jtrim.taskgraph.TaskGraphBuilder;
import org.jtrim.taskgraph.TaskGraphDefConfigurer;
import org.jtrim.utils.ExceptionHelper;

public final class CollectingTaskGraphDefConfigurer implements TaskGraphDefConfigurer {
    private final TaskGraphBuilderFactory graphBuilderFactory;

    private final Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> factoryDefs;

    public CollectingTaskGraphDefConfigurer(TaskGraphBuilderFactory graphBuilderFactory) {
        ExceptionHelper.checkNotNullArgument(graphBuilderFactory, "graphBuilderFactory");
        this.graphBuilderFactory = graphBuilderFactory;
        this.factoryDefs = new ConcurrentHashMap<>();
    }

    @Override
    public TaskFactoryDefiner factoryGroupDefiner(TaskFactoryGroupConfigurer groupConfigurer) {
        return new GroupDefiner(factoryDefs, groupConfigurer);
    }

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
        public <R, I> void defineFactory(TaskFactoryKey<R, I> defKey, TaskFactorySetup<R, I> setup) {
            TaskFactoryConfig<R, I> config = new TaskFactoryConfig<>(defKey, groupConfigurer, setup);
            factoryDefs.put(defKey, config);
        }
    }
}
