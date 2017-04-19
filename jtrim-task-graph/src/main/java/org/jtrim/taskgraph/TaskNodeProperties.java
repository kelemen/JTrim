package org.jtrim.taskgraph;

import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.utils.ExceptionHelper;

public class TaskNodeProperties {
    private final TaskExecutor executor;

    protected TaskNodeProperties(Builder builder) {
        this.executor = builder.executor;
    }

    public final TaskExecutor getExecutor() {
        return executor;
    }

    public static class Builder {
        private TaskExecutor executor;

        public Builder() {
            this.executor = SyncTaskExecutor.getSimpleExecutor();
        }

        public Builder(TaskNodeProperties defaults) {
            this.executor = defaults.getExecutor();
        }

        public final void setExecutor(TaskExecutor executor) {
            ExceptionHelper.checkNotNullArgument(executor, "executor");
            this.executor = executor;
        }

        public TaskNodeProperties build() {
            return new TaskNodeProperties(this);
        }
    }
}
