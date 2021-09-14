package org.jtrim2.jobprocessing;

import java.util.Objects;
import java.util.Optional;

public final class ConsumerCompletionStatus {
    public static final ConsumerCompletionStatus SUCCESS = new ConsumerCompletionStatus(false, null);
    public static final ConsumerCompletionStatus CANCELED = new ConsumerCompletionStatus(true, null);

    private final boolean canceled;
    private final Throwable error;

    private ConsumerCompletionStatus(boolean canceled, Throwable error) {
        this.canceled = canceled;
        this.error = error;
    }

    public static ConsumerCompletionStatus failed(Throwable error) {
        return new ConsumerCompletionStatus(false, Objects.requireNonNull(error, "error"));
    }

    public static ConsumerCompletionStatus of(boolean canceled, Throwable error) {
        if (error == null) {
            return canceled ? CANCELED : SUCCESS;
        } else {
            return new ConsumerCompletionStatus(canceled, error);
        }
    }

    public boolean isSuccess() {
        return !canceled && error == null;
    }

    public boolean isFailed() {
        return !canceled && error != null;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public Optional<Throwable> getErrorIfFailed() {
        return isFailed()
                ? Optional.of(error)
                : Optional.empty();
    }

    public Throwable tryGetError() {
        return error;
    }

    private String getStatusStr() {
        if (canceled) {
            return "CANCELED";
        } else if (error == null) {
            return "SUCCESS";
        } else {
            return "FAILED";
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("BatchCompletion{");
        result.append(getStatusStr());
        if (error != null) {
            result.append(", error=");
            result.append(error);
        }
        result.append('}');
        return result.toString();
    }
}
