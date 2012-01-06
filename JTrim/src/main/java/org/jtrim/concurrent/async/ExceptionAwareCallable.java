/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent.async;

import java.util.concurrent.Callable;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class ExceptionAwareCallable<T> implements Callable<T> {
    private final Callable<T> wrappedCallable;
    private final ExceptionListener<Callable<T>> listener;

    @SuppressWarnings("unchecked")
    public <V extends Callable<T>> ExceptionAwareCallable(V wrappedCallable, ExceptionListener<? super V> listener) {
        ExceptionHelper.checkNotNullArgument(wrappedCallable, "wrappedCallable");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.wrappedCallable = wrappedCallable;
        this.listener = (ExceptionListener<Callable<T>>) listener;
    }

    @Override
    public T call() throws Exception {
        try {
            return wrappedCallable.call();
        } catch (Throwable exception) {
            listener.onException(exception, wrappedCallable);
            throw exception;
        }
    }
}
