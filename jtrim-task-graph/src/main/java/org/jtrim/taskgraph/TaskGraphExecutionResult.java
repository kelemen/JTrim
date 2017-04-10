package org.jtrim.taskgraph;

public final class TaskGraphExecutionResult {
    private final boolean successful;

    private TaskGraphExecutionResult(Builder builder) {
        this.successful = builder.successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public static final class Builder {
        private boolean successful;

        public Builder() {
            this.successful = false;
        }

        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }

        public TaskGraphExecutionResult build() {
            return new TaskGraphExecutionResult(this);
        }
    }
}
