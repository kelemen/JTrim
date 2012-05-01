package org.jtrim.concurrent.executor;

/**
 *
 * @author Kelemen Attila
 */
public enum TaskState {
    NOT_STARTED(false),
    RUNNING(false),
    DONE_CANCELED(true),
    DONE_ERROR(true),
    DONE_COMPLETED(true);

    private final boolean done;

    private TaskState(boolean done) {
        this.done = done;
    }

    public boolean isDone() {
        return done;
    }
}
