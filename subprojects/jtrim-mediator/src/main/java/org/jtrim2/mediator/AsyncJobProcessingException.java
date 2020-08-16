package org.jtrim2.mediator;

import java.util.Objects;

public class AsyncJobProcessingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AsyncJobProcessingException(String message, Throwable cause) {
        super(message, Objects.requireNonNull(cause, "cause"));
    }

    public AsyncJobProcessingException(Throwable cause) {
        super(Objects.requireNonNull(cause, "cause"));
    }
}
