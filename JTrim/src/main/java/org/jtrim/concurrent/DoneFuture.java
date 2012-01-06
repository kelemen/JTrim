/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent;

import java.util.concurrent.*;

/**
 *
 * @author Kelemen Attila
 */
public final class DoneFuture<ResultType> implements Future<ResultType> {
    private final ResultType result;

    public DoneFuture(ResultType result) {
        this.result = result;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public ResultType get() {
        return result;
    }

    @Override
    public ResultType get(long timeout, TimeUnit unit) {
        return result;
    }
}
