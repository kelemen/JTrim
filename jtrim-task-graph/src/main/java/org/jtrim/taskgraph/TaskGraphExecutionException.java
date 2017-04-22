package org.jtrim.taskgraph;

public class TaskGraphExecutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TaskGraphExecutionException() {
    }

    public TaskGraphExecutionException(String message) {
        super(message);
    }

    public TaskGraphExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskGraphExecutionException(Throwable cause) {
        super(cause);
    }

    protected TaskGraphExecutionException(String message, Throwable cause, boolean writableStackTrace) {
        super(message, cause, true, writableStackTrace);
    }

    public static TaskGraphExecutionException withoutStackTrace(String message, Throwable cause) {
        return new TaskGraphExecutionException(message, cause, false);
    }
}
