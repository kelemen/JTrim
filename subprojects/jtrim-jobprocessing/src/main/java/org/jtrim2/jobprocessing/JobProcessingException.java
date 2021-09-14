package org.jtrim2.jobprocessing;

public class JobProcessingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JobProcessingException() {
    }

    public JobProcessingException(String message) {
        super(message);
    }

    public JobProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobProcessingException(Throwable cause) {
        super(cause);
    }
}
