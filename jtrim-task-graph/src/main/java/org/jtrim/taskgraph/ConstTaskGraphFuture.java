package org.jtrim.taskgraph;

import java.util.function.BiFunction;

final class ConstTaskGraphFuture<R> implements TaskGraphFuture<R> {
    private final R result;
    private final Throwable error;

    public ConstTaskGraphFuture(R result, Throwable error) {
        if (result != null && error != null) {
            throw new IllegalArgumentException("When there is a result, the error is expected to be null.");
        }

        this.result = result;
        this.error = error;
    }

    @Override
    public <R2> TaskGraphFuture<R2> onComplete(
            BiFunction<? super R, ? super Throwable, ? extends TaskGraphFuture<R2>> handler) {
        try {
            return handler.apply(result, error);
        } catch (Throwable ex) {
            return TaskGraphFuture.constFailure(ex);
        }
    }
}
