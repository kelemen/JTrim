package org.jtrim.taskgraph;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.utils.ExceptionHelper;

public class TaskGraphBuilderProperties {
    private static final Logger LOGGER = Logger.getLogger(TaskGraphBuilderProperties.class.getName());

    private final TaskFactoryProperties defaultFactoryProperties;

    private final TaskExecutor graphBuilderExecutor;
    private final TaskErrorHandler nodeCreateErrorHandler;

    protected TaskGraphBuilderProperties(Builder builder) {
        this.defaultFactoryProperties = builder.defaultFactoryProperties.build();
        this.graphBuilderExecutor = builder.graphBuilderExecutor;
        this.nodeCreateErrorHandler = builder.nodeCreateErrorHandler;
    }

    public TaskFactoryProperties getDefaultFactoryProperties() {
        return defaultFactoryProperties;
    }

    public final TaskExecutor getGraphBuilderExecutor() {
        return graphBuilderExecutor;
    }

    public final TaskErrorHandler getNodeCreateErrorHandler() {
        return nodeCreateErrorHandler;
    }

    private static void logNodeCreateError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
        LOGGER.log(Level.SEVERE, "Failure while creating node with key: " + nodeKey, error);
    }

    public static class Builder {
        private final TaskFactoryProperties.Builder defaultFactoryProperties;

        private TaskExecutor graphBuilderExecutor;
        private TaskErrorHandler nodeCreateErrorHandler;

        public Builder() {
            this.defaultFactoryProperties = new TaskFactoryProperties.Builder();
            this.graphBuilderExecutor = SyncTaskExecutor.getSimpleExecutor();
            this.nodeCreateErrorHandler = TaskGraphBuilderProperties::logNodeCreateError;
        }

        public Builder(TaskGraphBuilderProperties defaults) {
            this.defaultFactoryProperties = new TaskFactoryProperties.Builder(defaults.getDefaultFactoryProperties());
            this.graphBuilderExecutor = defaults.graphBuilderExecutor;
            this.nodeCreateErrorHandler = defaults.nodeCreateErrorHandler;
        }

        public TaskFactoryProperties.Builder defaultFactoryProperties() {
            return defaultFactoryProperties;
        }

        public final void setGraphBuilderExecutor(TaskExecutor graphBuilderExecutor) {
            ExceptionHelper.checkNotNullArgument(graphBuilderExecutor, "graphBuilderExecutor");
            this.graphBuilderExecutor = graphBuilderExecutor;
        }

        public final void setNodeCreateErrorHandler(TaskErrorHandler nodeCreateErrorHandler) {
            ExceptionHelper.checkNotNullArgument(nodeCreateErrorHandler, "nodeCreateErrorHandler");
            this.nodeCreateErrorHandler = nodeCreateErrorHandler;
        }

        public TaskGraphBuilderProperties build() {
            return new TaskGraphBuilderProperties(this);
        }
    }
}
