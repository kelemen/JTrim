package org.jtrim.taskgraph;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;

public class TaskGraphBuilderProperties {
    private static final Logger LOGGER = Logger.getLogger(TaskGraphBuilderProperties.class.getName());

    private final TaskFactoryProperties defaultFactoryProperties;

    private final TaskErrorHandler nodeCreateErrorHandler;

    protected TaskGraphBuilderProperties(Builder builder) {
        this.defaultFactoryProperties = builder.defaultFactoryProperties.build();
        this.nodeCreateErrorHandler = builder.nodeCreateErrorHandler;
    }

    public TaskFactoryProperties getDefaultFactoryProperties() {
        return defaultFactoryProperties;
    }

    public final TaskErrorHandler getNodeCreateErrorHandler() {
        return nodeCreateErrorHandler;
    }

    private static void logNodeCreateError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
        LOGGER.log(Level.SEVERE, "Failure while creating node with key: " + nodeKey, error);
    }

    public static class Builder {
        private final TaskFactoryProperties.Builder defaultFactoryProperties;

        private TaskErrorHandler nodeCreateErrorHandler;

        public Builder() {
            this.defaultFactoryProperties = new TaskFactoryProperties.Builder();
            this.nodeCreateErrorHandler = TaskGraphBuilderProperties::logNodeCreateError;
        }

        public Builder(TaskGraphBuilderProperties defaults) {
            this.defaultFactoryProperties = new TaskFactoryProperties.Builder(defaults.getDefaultFactoryProperties());
            this.nodeCreateErrorHandler = defaults.nodeCreateErrorHandler;
        }

        public TaskFactoryProperties.Builder defaultFactoryProperties() {
            return defaultFactoryProperties;
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
