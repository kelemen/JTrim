package org.jtrim.taskgraph;

public final class TaskGraphExecutionResult {
    private final boolean successful;
    private final boolean graphBuiltSuccessfully;

    private TaskGraphExecutionResult(Builder builder) {
        this.graphBuiltSuccessfully = builder.graphBuiltSuccessfully;
        this.successful = builder.successful;

        if (this.successful && !this.graphBuiltSuccessfully) {
            throw new IllegalStateException("Cannot be successful if graph building was unsuccessful.");
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    public boolean isGraphBuiltSuccessfully() {
        return graphBuiltSuccessfully;
    }

    public static final class Builder {
        private boolean successful;
        private boolean graphBuiltSuccessfully;

        public Builder() {
            this.graphBuiltSuccessfully = false;
            this.successful = false;
        }

        public void setGraphBuiltSuccessfully(boolean graphBuiltSuccessfully) {
            this.graphBuiltSuccessfully = graphBuiltSuccessfully;
        }

        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }

        public TaskGraphExecutionResult build() {
            return new TaskGraphExecutionResult(this);
        }
    }
}
