package org.jtrim.taskgraph;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.utils.ExceptionHelper;

public class TaskGraphExecProperties {
    private static final Logger LOGGER = Logger.getLogger(TaskGraphExecProperties.class.getName());

    private final TaskFactoryProperties defaultFactoryProperties;

    private final boolean stopOnFailure;
    private final TaskExecutor graphBuilderExecutor;

    private final TaskErrorHandler nodeCreateErrorHandler;
    private final TaskErrorHandler computeErrorHandler;
    private final Consumer<? super Throwable> otherErrorHandler;

    private TaskGraphExecProperties(Builder builder) {
        this.defaultFactoryProperties = builder.defaultFactoryProperties.build();
        this.stopOnFailure = builder.stopOnFailure;
        this.graphBuilderExecutor = builder.graphBuilderExecutor;
        this.nodeCreateErrorHandler = builder.nodeCreateErrorHandler;
        this.computeErrorHandler = builder.computeErrorHandler;
        this.otherErrorHandler = builder.otherErrorHandler;
    }

    public TaskFactoryProperties getDefaultFactoryProperties() {
        return defaultFactoryProperties;
    }

    public final boolean isStopOnFailure() {
        return stopOnFailure;
    }

    public final TaskExecutor getGraphBuilderExecutor() {
        return graphBuilderExecutor;
    }

    public final TaskErrorHandler getNodeCreateErrorHandler() {
        return nodeCreateErrorHandler;
    }

    public final TaskErrorHandler getComputeErrorHandler() {
        return computeErrorHandler;
    }

    public final Consumer<? super Throwable> getOtherErrorHandler() {
        return otherErrorHandler;
    }

    private static void logNodeCreateError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
        LOGGER.log(Level.SEVERE, "Failure while creating node with key: " + nodeKey, error);
    }

    private static void logNodeComputeError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
        LOGGER.log(Level.SEVERE, "Failure while creating node with key: " + nodeKey, error);
    }

    private static void logOtherError(Throwable error) {
        LOGGER.log(Level.SEVERE, "Unexpected failure during task graph execution.", error);
    }

    public static class Builder {
        private final TaskFactoryProperties.Builder defaultFactoryProperties;
        private boolean stopOnFailure;
        private TaskExecutor graphBuilderExecutor;

        private TaskErrorHandler nodeCreateErrorHandler;
        private TaskErrorHandler computeErrorHandler;
        private Consumer<? super Throwable> otherErrorHandler;

        public Builder() {
            this.defaultFactoryProperties = new TaskFactoryProperties.Builder();
            this.stopOnFailure = false;
            this.graphBuilderExecutor = SyncTaskExecutor.getSimpleExecutor();
            this.nodeCreateErrorHandler = TaskGraphExecProperties::logNodeCreateError;
            this.computeErrorHandler = TaskGraphExecProperties::logNodeComputeError;
            this.otherErrorHandler = TaskGraphExecProperties::logOtherError;
        }

        public Builder(TaskGraphExecProperties defaults) {
            this.defaultFactoryProperties = new TaskFactoryProperties.Builder(defaults.getDefaultFactoryProperties());
            this.stopOnFailure = defaults.isStopOnFailure();
        }

        public TaskFactoryProperties.Builder defaultFactoryProperties() {
            return defaultFactoryProperties;
        }

        public final void setStopOnFailure(boolean stopOnFailure) {
            this.stopOnFailure = stopOnFailure;
        }

        public final void setGraphBuilderExecutor(TaskExecutor graphBuilderExecutor) {
            ExceptionHelper.checkNotNullArgument(graphBuilderExecutor, "graphBuilderExecutor");
            this.graphBuilderExecutor = graphBuilderExecutor;
        }

        public final void setNodeCreateErrorHandler(TaskErrorHandler nodeCreateErrorHandler) {
            ExceptionHelper.checkNotNullArgument(nodeCreateErrorHandler, "nodeCreateErrorHandler");
            this.nodeCreateErrorHandler = nodeCreateErrorHandler;
        }

        public final void setComputeErrorHandler(TaskErrorHandler computeErrorHandler) {
            ExceptionHelper.checkNotNullArgument(computeErrorHandler, "computeErrorHandler");
            this.computeErrorHandler = computeErrorHandler;
        }

        public final void setOtherErrorHandler(Consumer<? super Throwable> otherErrorHandler) {
            ExceptionHelper.checkNotNullArgument(otherErrorHandler, "otherErrorHandler");
            this.otherErrorHandler = otherErrorHandler;
        }

        public TaskGraphExecProperties build() {
            return new TaskGraphExecProperties(this);
        }
    }
}
