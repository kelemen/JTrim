package org.jtrim.taskgraph;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;

public class TaskGraphExecutorProperties {
    private static final Logger LOGGER = Logger.getLogger(TaskGraphExecProperties.class.getName());

    private final boolean stopOnFailure;
    private final TaskErrorHandler computeErrorHandler;

    protected TaskGraphExecutorProperties(Builder builder) {
        this.stopOnFailure = builder.stopOnFailure;
        this.computeErrorHandler = builder.computeErrorHandler;
    }

    public final boolean isStopOnFailure() {
        return stopOnFailure;
    }

    public final TaskErrorHandler getComputeErrorHandler() {
        return computeErrorHandler;
    }

    private static void logNodeComputeError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
        LOGGER.log(Level.SEVERE, "Failure while creating node with key: " + nodeKey, error);
    }

    public static class Builder {
        private boolean stopOnFailure;
        private TaskErrorHandler computeErrorHandler;

        public Builder() {
            this.stopOnFailure = false;
            this.computeErrorHandler = TaskGraphExecutorProperties::logNodeComputeError;
        }

        public Builder(TaskGraphExecProperties defaults) {
            this.stopOnFailure = defaults.isStopOnFailure();
        }

        public final void setStopOnFailure(boolean stopOnFailure) {
            this.stopOnFailure = stopOnFailure;
        }

        public final void setComputeErrorHandler(TaskErrorHandler computeErrorHandler) {
            ExceptionHelper.checkNotNullArgument(computeErrorHandler, "computeErrorHandler");
            this.computeErrorHandler = computeErrorHandler;
        }

        public TaskGraphExecutorProperties build() {
            return new TaskGraphExecutorProperties(this);
        }
    }
}
