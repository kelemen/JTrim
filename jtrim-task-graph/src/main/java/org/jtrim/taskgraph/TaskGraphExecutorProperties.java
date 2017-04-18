package org.jtrim.taskgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;

public class TaskGraphExecutorProperties {
    private static final Logger LOGGER = Logger.getLogger(TaskGraphExecutorProperties.class.getName());

    private final boolean stopOnFailure;
    private final TaskErrorHandler computeErrorHandler;
    private final Set<TaskNodeKey<?, ?>> resultNodeKeys;

    protected TaskGraphExecutorProperties(Builder builder) {
        this.stopOnFailure = builder.stopOnFailure;
        this.computeErrorHandler = builder.computeErrorHandler;
        this.resultNodeKeys = builder.resultNodeKeys.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(builder.resultNodeKeys));
    }

    public final boolean isStopOnFailure() {
        return stopOnFailure;
    }

    public final TaskErrorHandler getComputeErrorHandler() {
        return computeErrorHandler;
    }

    public final Set<TaskNodeKey<?, ?>> getResultNodeKeys() {
        return resultNodeKeys;
    }

    private static void logNodeComputeError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
        LOGGER.log(Level.SEVERE, "Failure while creating node with key: " + nodeKey, error);
    }

    public static class Builder {
        private boolean stopOnFailure;
        private TaskErrorHandler computeErrorHandler;
        private final Set<TaskNodeKey<?, ?>> resultNodeKeys;

        public Builder() {
            this.stopOnFailure = false;
            this.computeErrorHandler = TaskGraphExecutorProperties::logNodeComputeError;
            this.resultNodeKeys = new HashSet<>();
        }

        public Builder(TaskGraphExecutorProperties defaults) {
            this.stopOnFailure = defaults.isStopOnFailure();
            this.computeErrorHandler = defaults.getComputeErrorHandler();
            this.resultNodeKeys = new HashSet<>(defaults.getResultNodeKeys());
        }

        public final void addResultNodeKey(TaskNodeKey<?, ?> nodeKey) {
            ExceptionHelper.checkNotNullArgument(nodeKey, "nodeKey");
            this.resultNodeKeys.add(nodeKey);
        }

        public final void addResultNodeKeys(Collection<? extends TaskNodeKey<?, ?>> nodeKeys) {
            ExceptionHelper.checkNotNullElements(nodeKeys, "nodeKeys");
            this.resultNodeKeys.addAll(nodeKeys);
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
