package org.jtrim2.stream;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.AsyncTasks;

final class ExceptionCollector {
    private static final Throwable POISON = OperationCanceledException.withoutStackTrace("POISON", null);

    private final AtomicReference<Throwable> firstFailureRef;

    public ExceptionCollector() {
        this.firstFailureRef = new AtomicReference<>();
    }

    public static Throwable updateException(Throwable mainEx, Throwable newEx) {
        if (newEx == null || newEx == mainEx) {
            return mainEx;
        }

        if (mainEx == null) {
            return newEx;
        } else {
            Throwable preferredEx;
            Throwable secondaryEx;
            if (AsyncTasks.isCanceled(mainEx)) {
                preferredEx = newEx;
                secondaryEx = mainEx;
            } else {
                preferredEx = mainEx;
                secondaryEx = newEx;
            }
            preferredEx.addSuppressed(secondaryEx);
            return preferredEx;
        }
    }

    public void setFirstFailure(Throwable failure) {
        firstFailureRef.compareAndSet(null, failure);
    }

    public Throwable getLatest() {
        Throwable currentEx = firstFailureRef.get();
        return currentEx == POISON ? null : currentEx;
    }

    public Throwable consumeLatestAndGet() {
        Throwable currentEx = firstFailureRef.get();
        if (currentEx == null || currentEx == POISON) {
            return null;
        }

        currentEx = firstFailureRef.getAndSet(POISON);
        return currentEx == POISON ? null : currentEx;
    }

    public Throwable consumeLatestAndUpdate(Throwable mainEx) {
        Throwable current = consumeLatestAndGet();
        return updateException(mainEx, current);
    }
}
