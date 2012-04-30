package org.jtrim.concurrent.executor;

/**
 *
 * @author Kelemen Attila
 */
public class TaskCanceledException extends RuntimeException {
    private static final long serialVersionUID = 6512650623463624418L;

    public TaskCanceledException(Throwable cause) {
        super(cause);
    }

    public TaskCanceledException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskCanceledException(String message) {
        super(message);
    }

    public TaskCanceledException() {
    }
}
