package org.jtrim2.executor;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @see ExecutorsEx#canceledFuture()
 */
enum CanceledFuture implements Future<Object> {
    INSTANCE;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return true;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public Object get() {
        throw new CancellationException();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) {
        return get();
    }
}
