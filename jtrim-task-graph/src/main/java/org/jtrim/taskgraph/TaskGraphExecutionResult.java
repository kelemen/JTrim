package org.jtrim.taskgraph;

public final class TaskGraphExecutionResult {
    private final boolean errored;
    private final boolean fullyCompleted;

    private TaskGraphExecutionResult(Builder builder) {
        this.errored = builder.errored;
        this.fullyCompleted = builder.fullyCompleted;
    }

    public boolean isErrored() {
        return errored;
    }

    public boolean isFullyCompleted() {
        return fullyCompleted;
    }

    public boolean isCanceled() {
        return !errored && !fullyCompleted;
    }

    public static final class Builder {
        private boolean errored;
        private boolean fullyCompleted;

        public Builder() {
            this.errored = false;
            this.fullyCompleted = false;
        }

        public void setErrored(boolean errored) {
            this.errored = errored;
        }

        public void setFullyCompleted(boolean fullyCompleted) {
            this.fullyCompleted = fullyCompleted;
        }

        public TaskGraphExecutionResult build() {
            return new TaskGraphExecutionResult(this);
        }
    }
}
