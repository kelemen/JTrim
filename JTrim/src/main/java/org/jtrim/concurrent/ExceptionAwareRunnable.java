/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import org.jtrim.utils.ExceptionHelper;


/**
 *
 * @author Kelemen Attila
 */
public final class ExceptionAwareRunnable implements Runnable {
    private final Runnable wrappedRunnable;
    private final ExceptionListener<Runnable> listener;

    @SuppressWarnings("unchecked")
    public <V extends Runnable> ExceptionAwareRunnable(V wrappedRunnable,
    ExceptionListener<? super V> listener) {

        ExceptionHelper.checkNotNullArgument(wrappedRunnable, "wrappedRunnable");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.wrappedRunnable = wrappedRunnable;
        this.listener = (ExceptionListener<Runnable>) listener;
    }

    @Override
    public void run() {
        try {
            wrappedRunnable.run();
        } catch (Throwable exception) {
            listener.onException(exception, wrappedRunnable);
            throw exception;
        }
    }
}
