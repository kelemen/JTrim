/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent;

import java.util.concurrent.*;

/**
 * @see ExecutorsEx#canceledFuture()
 *
 * @author Kelemen Attila
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
